# Migrating off Spring Data REST to plain Spring MVC + Spring Data JPA

Status: **plan / feasibility assessment** — no production code changed yet.

Guiding principle, agreed with the maintainer: **the easiest code that satisfies the contract wins.**
Where this plan had two options, the simpler one is now chosen outright rather than left open.

## 1. Verdict

The migration is feasible and low-risk *in terms of behaviour*, because the Gherkin suite
(24 feature files, 163 scenarios, plus 4 Keycloak-backed `*IT` tests) pins the HTTP contract
end-to-end and is written entirely against `MockMvc`/OkHttp — not against Spring Data REST APIs.
That suite is the safety net.

It is not a small job. Spring Data REST silently provides seven things this codebase depends on:

1. CRUD endpoints for 6 aggregates (~35 routes).
2. HAL rendering — `_embedded.<rel>`, `_links.self`, `page` metadata.
3. **URI-as-foreign-key** in JSON bodies (`"school": "/v0/schools/{id}"`) via `UriToEntityConverter`
   — **being dropped, not reproduced** (§5.1).
4. **`text/uri-list` association endpoints** (`POST /classrooms/{id}/teachers`, `PUT .../tutor`, …)
   — **being dropped, not reproduced** (§5.1).
5. The **repository event model** (`@HandleBeforeCreate/Save/Delete/LinkSave`) — where *all*
   authorization and every business invariant currently lives.
6. Entity validation via `ValidatingRepositoryEventListener`, producing the `errors[]` ProblemDetail shape.
7. The `/v0` base path, wired into `SpringSecurityConfiguration` via `RepositoryRestConfiguration`.

The codebase is already half-migrated: `KidsCollectionController`, `PersonUpdateController` and
`PersonRoleController` are hand-written controllers that exist precisely because Spring Data REST got
in the way — direct evidence for the premise behind this migration.

Size: **~68 new/modified main classes**, **17 ArchUnit rule files** (14 rewritten/extended, 3 new,
~55 rules total), 2 Checkstyle configs, ~38 test files touched, 17 lines of Gherkin (16 for validation
paths, 1 for a scope tightening), 12 step-definition methods and 1 `*IT` method.

> **Read §11 first.** The security audit found **three live defects in `main`** that this migration
> did not cause: `POST /v0/classrooms` is not scope-gated at all, no `*.delete` scope exists in the
> identity provider so every DELETE endpoint is unreachable in a real deployment, and association
> reads leak across scopes. They are fixable in a separate commit before Phase 1 and should not wait
> for the migration.

---

## 2. What has to be replaced, precisely

### 2.1 Deleted outright

| Class | Why |
|---|---|
| `config/web/RestExposureConfiguration` | `RepositoryRestConfigurer`; exposure rules become explicit routes |
| `config/web/ValidationConfiguration` | `ValidatingRepositoryEventListener` → `@Valid` on request DTOs |
| all 14 `*/handlers/*EventHandler` + `*Handler` classes | logic moves to `*Policy`, does not vanish |
| all 6 `*/handlers/*HandlersConfiguration` | replaced by slice configurations |

`classroom/handlers/TeacherSchoolMembership` is pure domain logic and survives, relocated.

### 2.2 Behaviour that must move, not disappear

The `handlers` package *is* the authorization layer:

| Current handler | Becomes |
|---|---|
| `KidAuthorizationEventHandler` (beforeCreate) | `KidAdapter.create` resolves the authenticated parent, 403 if none |
| `KidUpdateAuthorizationEventHandler` (beforeSave) | `KidPolicy.authorizeUpdate(kid, newParent, newClassroom)` |
| `KidDeleteAuthorizationEventHandler` | `KidPolicy.authorizeDelete` |
| `ParentRegistrationHandler` | `ParentAdapter.register` — binds JWT subject → `Person` |
| `ParentUpdateAuthorizationEventHandler` (beforeSave + **beforeLinkSave**) | `ParentPolicy.authorizeUpdate` / `.authorizeKidBinding` |
| `ParentDeleteEventHandler` | `ParentPolicy.authorizeDelete` + `ParentHasKidsException` guard |
| `TeacherRegistrationHandler` / `TeacherUpdate…` / `TeacherDelete…` | `TeacherAdapter` + `TeacherPolicy` |
| `SchoolUpdate…` / `SchoolDelete…` | `SchoolPolicy` |
| `ClassroomUpdateAuthorizationEventHandler` (beforeSave + beforeLinkSave) | `ClassroomPolicy` |
| `ClassroomTeacherAssignmentEventHandler` | `ClassroomAdapter` invariant check on create + staff mutation |

**Order of operations must be preserved exactly**, because the scenarios assert the distinction:
`load → 404 if absent → authorize → 403 → invariant → 409/422 → validate → 400 → persist → 2xx`.
`kid/update.feature` expects 404 for a missing kid but 403 for another parent's kid, so the lookup
must precede authorization.

### 2.3 Simplifications the migration buys

- `Kid.previousParentId`, `Kid.previousClassroomId`, `Classroom.previousTutorId` are `@Transient`
  `@PostLoad` fields that exist *only* because Spring Data REST mutates a loaded entity in place.
  With explicit adapters the old and new values are both in scope at the call site.
  **All three fields and both `@PostLoad` methods are deleted.**
- **Bean Validation annotations leave the entities** (§5.4) — `@NotBlank`, `@Size`, `@Email`,
  `@Past`, `@Positive`, `@Max`, `@Pattern`, `@Valid`, `@NotNull` across 6 entities.
- **Jackson annotations leave the entities** — every `@JsonIgnore` on ids, audit fields and
  transients becomes dead once DTOs own the wire format.

Net effect: entities become plain JPA mappings, which is what `JpaEntityRulesTest` should have been
able to assume all along.

---

## 3. Target architecture

### 3.1 The adapter layer

Confirmed direction: **an adapter sits between the controller and the repository** and owns the
entity ⇄ representation translation, matching the original brief ("Jackson and MapStruct for JSON
mapping and adapters"). Layering per slice:

```
Controller ──► Adapter ──► Repository
   HTTP only     │           data access
                 ├── Mapper  (MapStruct, pure field mapping)
                 └── Policy  (authorization)
```

```
edu.lyra.members.api.<aggregate>/
├── <Aggregate>.java                       JPA entity — plain mapping, no Jackson, no validation
├── <Aggregate>Repository.java             Spring Data JPA
└── rest/                                  internal to the slice, package-private
    ├── <Aggregate>Controller.java         @RequestMapping type-level, registered as a @Bean
    ├── <Aggregate>Adapter.java            repo + policy + mapper orchestration; builds links
    ├── <Aggregate>Mapper.java             MapStruct, entity ⇄ model/request
    ├── <Aggregate>Model.java              extends RepresentationModel<…>, @Relation   (outbound)
    ├── <Aggregate>Request.java            record, Jakarta constraints                 (inbound, create)
    ├── <Aggregate>PatchRequest.java       record                                      (inbound, patch)
    ├── <Aggregate>Policy.java             authorization
    └── <Aggregate>RestConfiguration.java  explicit @Bean wiring
```

The old `..handlers` package disappears entirely — one internal package per slice instead of two.

### 3.2 Inbound vs outbound: the asymmetry is deliberate

| Direction | Type | Coupling |
|---|---|---|
| **inbound** (`*Request`) | `record` | Jackson + Jakarta Validation only. **No Spring HATEOAS** — clients never send links. Associations are `UUID` fields (§5.1) |
| **outbound** (`*Model`) | class `extends RepresentationModel<XModel>` | Spring HATEOAS; carries `_links` built with `WebMvcLinkBuilder`, **plus an exposed `id`** so clients can reference the resource without parsing hrefs (§5.1) |

This resolves the tension from the previous revision. My earlier recommendation to keep response
models as plain records was driven by a MapStruct concern that is smaller than I stated: MapStruct
derives target properties from setters, and `RepresentationModel` exposes links only through
`getLinks()` (returning `Links`, which is not a `Collection`) and `add(…)`, so it most likely is not
seen as a writable property at all. **If** the processor does flag it, the fix is one
`@Mapping(target = "links", ignore = true)` per mapping method. That is not a reason to give up
hypermedia on the model. Confirm either way in the Phase 0 spike.

### 3.3 How the pieces cooperate

`<Aggregate>Adapter implements RepresentationModelAssembler<X, XModel>`, so it plugs straight into
`PagedResourcesAssembler.toModel(page, adapter)` — the same call `KidsCollectionController` already
makes today with `PersistentEntityResourceAssembler`. `toModel(X)` calls the MapStruct mapper for
the fields and then adds links. One class, idiomatic, no separate assembler type.

| Concern | Spring Data REST (today) | Target |
|---|---|---|
| item payload | `PersistentEntityResource` | `XModel extends RepresentationModel<XModel>` |
| collection payload | `PagedModel<PersistentEntityResource>` | `PagedModel<XModel>` |
| assembling | `PersistentEntityResourceAssembler` | `XAdapter implements RepresentationModelAssembler` |
| paging | `PagedResourcesAssembler` | `PagedResourcesAssembler` *(unchanged)* |
| link building | implicit | `WebMvcLinkBuilder.linkTo(methodOn(…))` |
| relation naming | derived from entity | `@Relation` on `XModel` |

**Mappers stay pure.** URI→entity resolution (§5.1) happens in the *adapter*, which then hands the
mapper an already-resolved entity. That keeps `XMapper` free of repositories and of
`AuthenticatedPrincipal`, which is worth an ArchUnit rule (§7.3) and makes mapper unit tests trivial.

### 3.4 Cross-slice access

Repositories stay `public` and remain the cross-slice port — `PersonRoleAdapter` legitimately needs
`ParentRepository` and `TeacherRepository`. Inventing a per-slice service interface purely to route
that would add a layer for one call site, against the simplicity guideline. The decoupling the brief
asks for is enforced instead by "**controllers never touch repositories; only adapters do**"
(§7.1/§7.4), which is the constraint that actually matters.

### 3.5 Shared web infrastructure (`config/web/`)

| Component | Purpose |
|---|---|
| `ApiBasePath` (`@ConfigurationProperties`) | replaces `RepositoryRestConfiguration.getBasePath()`; feeds security config *and* link building |
| `ProblemDetailsControllerAdvice` *(modified)* | handles `MethodArgumentNotValidException` instead of `RepositoryConstraintViolationException` |
| `RootController` | reinstates the `GET /v0/` index of collection links |

Deliberately **not** built, thanks to §5.1: no `text/uri-list` message converter and no URI→entity
resolver. Association references are plain `UUID`s the adapter looks up directly.

---

## 4. Endpoint inventory to reproduce

~35 routes, from `SpringSecurityConfiguration`, the step definitions and the `*IT` tests.

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/v0/parents`, `/{id}` | 200 | paged HAL |
| POST | `/v0/parents` | 201 + `Location` | binds JWT subject to `Person` |
| PATCH | `/v0/parents/{id}` | 204 / 404 | delegating `name`/`surname`/`mail` |
| DELETE | `/v0/parents/{id}` | 204 / 409 | 409 if kids linked |
| **PUT** | **`/v0/parents/{id}/kids/{kidId}`** | 204 | *was* `POST …/kids` + `text/uri-list` (§5.1) |
| GET | `/v0/kids` | 200 | **visibility-filtered** (admin / parent / teacher / none) |
| GET | `/v0/kids/{id}` | 200 / 404 | |
| POST | `/v0/kids` | 201 + `Location` | assigns authenticated parent |
| PATCH | `/v0/kids/{id}` | 204 / 403 / 404 | `parent`, `classroom` **as ids** (§5.1) |
| DELETE | `/v0/kids/{id}` | 204 | |
| GET | `/v0/schools`, `/{id}` | 200 | |
| POST / PATCH / DELETE | `/v0/schools[/{id}]` | 201 / 204 / 409 | 409 if classrooms or teachers linked |
| GET | `/v0/teachers`, `/{id}` | 200 | |
| POST | `/v0/teachers` | 201 | `school` as id |
| PATCH / DELETE | `/v0/teachers/{id}` | 204 / 409 | 409 if tutoring/teaching |
| GET | `/v0/classrooms`, `/{id}` | 200 | |
| POST | `/v0/classrooms` | 201 / 422 | `school` + `tutor` as ids |
| PATCH / DELETE | `/v0/classrooms/{id}` | 204 / 409 | 409 if kids enrolled |
| GET | `/v0/classrooms/{id}/teachers` | 200 | `_embedded.teachers` |
| **PUT** | **`/v0/classrooms/{id}/teachers/{teacherId}`** | 204 / 404 / 422 | *was* `POST` + `text/uri-list`; **needs a new security matcher** (§5.2.1) |
| GET | `/v0/classrooms/{id}/tutor` | 200 / **404 when unset** | |
| **PUT** | **`/v0/classrooms/{id}/tutor/{teacherId}`** | 204 / 404 / 422 | *was* `PUT …/tutor` + `text/uri-list`; **path changed, needs a new security matcher** (§5.2.1) |
| **PUT** | **`/v0/classrooms/{id}/kids/{kidId}`** | 204 | *was* `POST` + `text/uri-list`; **needs a new security matcher** |
| GET | `/v0/persons`, `/{id}` | 200 | admin only |
| PUT / DELETE | `/v0/persons/{id}/{parent,teacher}` | 204 / 400 / 404 / 409 | already hand-written |

**Decided:** reproduce the association GETs that back emitted `_links` rels
(`/parents/{id}/kids`, `/kids/{id}/parent`, `/kids/{id}/classroom`, `/teachers/{id}/school`,
`/schools/{id}/classrooms`, `/schools/{id}/teachers`, `/classrooms/{id}/school`); **drop
`/v0/profile`** (ALPS, no consumer). Reinstate `GET /v0/` as a link index.

---

## 5. Compatibility risks and decisions

### 5.1 Association references — **decided: drop URIs, use ids**

Spring Data REST expresses associations two ways, and **neither is required by HATEOAS**:

- **URI strings inside JSON bodies** — `POST /v0/teachers` sends `{"school": "/v0/schools/{uuid}"}`,
  `PATCH /v0/kids/{id}` sends `{"parent": "/v0/parents/{uuid}"}`. This has **no standard basis at
  all**; it is `UriToEntityConverter` being convenient. HAL specifies response format only and says
  nothing about request bodies. (JSON:API, for contrast, uses `{"data": {"type": …, "id": …}}`.)
- **`text/uri-list` association sub-resources** — the media type itself is real (RFC 2483 §5), but
  "manage an association by POSTing a uri-list to a sub-resource" is a Spring Data REST invention,
  not a REST or hypermedia convention.

HATEOAS constrains *responses*: links tell a client what it may do next. It imposes nothing on how a
client names a resource when writing. So both go.

**There is a genuine argument for URI references that must be answered, not ignored:** today entity
ids are `@JsonIgnore`d, so the *only* way a client learns a school's identity is `_links.self.href`.
Switching to bare ids without changing responses would force every client to string-parse hrefs —
strictly worse than what we have. The replacement is therefore a **pair**:

| | Before | After |
|---|---|---|
| response | id hidden; identity only via `_links.self.href` | **`id` exposed on the model**, `_links.self` retained for navigation |
| body reference | `"school": "/v0/schools/{uuid}"` | `"school": "{uuid}"` |
| collection membership | `POST /classrooms/{id}/teachers`, `text/uri-list` | `PUT /classrooms/{id}/teachers/{teacherId}`, no body |
| single-valued association | `PUT /classrooms/{id}/tutor`, `text/uri-list` | `PUT /classrooms/{id}/tutor/{teacherId}`, no body |

Exposing `id` alongside `_links` is what most HAL APIs do and nothing in HAL forbids it.

**Every association write takes the same shape**, regardless of cardinality:

```
PUT /v0/{owner}/{ownerId}/{relation}/{targetId}      → 204, no request body
```

so `…/classrooms/{id}/teachers/{teacherId}`, `…/classrooms/{id}/tutor/{teacherId}`,
`…/classrooms/{id}/kids/{kidId}` and `…/parents/{id}/kids/{kidId}` are one pattern, not two. A
single-valued relation could equally have been `PUT …/tutor` with a small JSON body, but splitting the
two cardinalities buys nothing and costs a second controller shape, a request record, a second
security-matcher shape and a second test shape. `PUT …/tutor/{teacherId}` reads as "set the tutor
relation to this teacher" and keeps one pattern end to end.

`PUT` also makes every one of these idempotent, which `POST` never was.

**What this deletes from the plan:**

- `UriListHttpMessageConverter` and `UriListHttpMessageConverterTest` — gone entirely; no custom
  media type, no parser.
- `EntityUriResolver` and `EntityUriResolverTest` — gone; request records carry `UUID` fields and the
  adapter calls `repository.findById` directly.
- `PersonRoleController.school(Map<String, Object>)` and its
  `@Qualifier("defaultConversionService") ConversionService` — replaced by a request record with a
  `@NotNull UUID school`.
- **Every request DTO for an association write.** With the uniform path form, no association endpoint
  has a body at all: all four are `@PathVariable`-only, with no `@RequestBody`, no `@Valid` and no
  record to declare. A 404 for an unknown target replaces what would have been a 400 for an unknown
  id in a body.

**Validation behaviour is unchanged.** `@NotNull UUID school` yields field `school` / "must not be
null" (`teacher/create.feature`), and a missing school on `PUT /persons/{id}/teacher` still yields
400 (`person/roles.feature`) — now through `MethodArgumentNotValidException` rather than a caught
`ConversionException`. An id that parses but matches nothing → 400, as before.

### 5.2 Cost and consequences of §5.1

**Zero Gherkin changes.** Every affected scenario is phrased behaviourally — "I add teacher
\"Marta Ibáñez\" to the classroom", "I create a classroom … at school \"Gloria Fuertes\" with tutor
\"Pablo Ruiz\"", "I bind kid … to parent …". No feature file mentions a URI or a media type.

**11 step-definition methods across 5 files, plus 1 `*IT` method:**

| File | Methods |
|---|---|
| `cucumber/classroom/ClassroomCreationFeatures` | 6 — `createClassroom`, `createClassroomWithTutor`, `performAddTeacher`, `addTeacherToNonExistentClassroom`, `performSetTutor`, `enrollKidInClassroom` |
| `cucumber/kid/KidUpdateFeatures` | 2 — `body.put("parent", …)`, `body.put("classroom", …)` |
| `cucumber/parent/ParentUpdateFeatures` | 1 — `bindKidToParent` |
| `cucumber/person/PersonRoleFeatures` | 1 — `schoolBody` |
| `cucumber/teacher/TeacherCreationFeatures` | 1 — the school field |
| `TeacherIT` | 1 — sends the school `Location` header as the `school` value |

The two `URI_LIST` `MediaType` constants (in `ClassroomCreationFeatures` and `ParentUpdateFeatures`)
are deleted outright.

Each becomes `location.substring(location.lastIndexOf('/') + 1)`, a helper these classes already have.

**Be honest about the safety cost.** For these endpoints this stops being a like-for-like migration
and becomes a deliberate API redesign, with two consequences:

1. Any existing client breaks. **Confirmed: there are none**, so this cost is zero.
2. Where a step definition changes, the scenario stops being an independent regression net — a bug in
   the controller and a matching bug in the step definition would cancel out. `TeacherIT` normally
   backstops this, but it changes too.

**Mitigation:** write the new `<X>ControllerTest` cases (§9.3) from the endpoint table in §4 *before*
touching any step definition, so the expected status codes and paths are pinned by a test that was
written against the spec rather than against the implementation.

### 5.2.1 Security matcher ordering — a trap this creates

`SpringSecurityConfiguration` matchers are **first-match-wins**, and the new association paths sit
underneath existing wildcard rules. Three concrete hazards:

- **`PUT /v0/classrooms/{id}/tutor/{teacherId}` silently orphans an existing matcher.** The current
  rule is `PUT /v0/classrooms/*/tutor` — two segments. The new path has three, so the rule stops
  matching *without being deleted*: it still reads as protection while protecting nothing. A dead
  matcher is worse than a missing one, because it looks correct in review.
- **`PUT /v0/classrooms/{id}/teachers/{teacherId}` matches no rule at all**, nor do
  `/classrooms/*/kids/*` and `/parents/*/kids/*`. All four fall through to
  `.anyRequest().authenticated()` — **any authenticated caller could alter a classroom's teaching
  staff, its tutor or its roster.** Matchers requiring `classrooms.update` / `parents.update` must be
  added for every one.
- **If association *removal* is ever added**, `DELETE /v0/classrooms/{id}/teachers/{teacherId}`
  matches the existing `DELETE /v0/classrooms/**` rule and would demand `classrooms.delete` — the
  wrong scope for what is an update. Same for `…/tutor`, `…/kids/{kidId}` and
  `/parents/{id}/kids/{kidId}`. The specific matchers must be registered **before** the wildcard.

Removal endpoints are out of scope (no scenario covers them), but the constraint is recorded here so
whoever adds them does not trip over it.

All three hazards are instances of the same root cause — the chain fails open. **`denyAll` (§11.1 F4)
turns every one of them from a silent hole into a 403 on the first request**, and
`EndpointCoverageTest` (§11.6) catches the orphaned-matcher case at build time. This is the single
best argument for that change: the tutor path here would otherwise be a live regression introduced by
a refactor that looked purely cosmetic.

### 5.3 PATCH partial-merge semantics

`DomainObjectReader` merges only properties present in the body. Replicating it means distinguishing
*absent* from *explicitly null*.

**Plan:** MapStruct `@MappingTarget` +
`@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)` — `null`
means "not supplied".

**Known narrowing:** a field cannot be `PATCH`ed to `null`. Checked against every scenario: explicit
nulls appear only on **create** (`parent/create.feature`, `teacher/create.feature`), never on PATCH.
No scenario regresses. Document it in the README; switch the affected fields to `JsonNullable<T>` only
if a client ever needs it.

### 5.4 Validation — **decided: validate the request DTO**

Per the simplicity guideline, validation moves to the edge:

- `@Valid @RequestBody XRequest` with Jakarta constraints **on the record components**.
- **All Bean Validation annotations are removed from the entities** (§2.3). The database keeps
  `nullable`/`length`/`unique` as the real backstop, and `DataIntegrityViolationException` → 409 is
  already wired.
- `ProblemDetailsControllerAdvice` overrides `handleMethodArgumentNotValid` to emit the existing
  `errors[{entity, property, message}]` array, so `CommonAssertions`'
  `$.errors[?(@.property == '…' && @.message == '…')]` JSONPath keeps working unchanged.

**Contract change:** error paths lose the leaked `person.` prefix. `person.name` → `name`,
`person.surname` → `surname`, `person.mail` → `mail`. The wire format has no `person` object, so this
is a correction, not a regression. Constraint *messages* are unchanged ("must not be blank",
"size must be between 0 and 100", "must be a well-formed email address"), because the same
annotations produce them.

**Exactly 16 Gherkin lines change**, in 4 files — all mechanical:

| File | Lines |
|---|---|
| `parent/create.feature` | 26, 27 (Examples table), 34, 41, 48, 62, 76 |
| `parent/update.feature` | 43 |
| `teacher/create.feature` | 28, 29 (Examples table), 37, 45, 53, 68, 83 |
| `teacher/update.feature` | 45 |

No other feature file mentions `person.`. School (`name`), classroom (`course`, `group`) and teacher
(`school`) paths are already flat and stay identical.

**Do this in the Teacher/Parent slice commits, not as a big-bang** — each edit lands next to the code
that causes it, so review can see cause and effect.

### 5.5 HAL relation names — silent breakage risk

Tests assert `_embedded.kids`, `_embedded.schools`, `_embedded.teachers`, `_embedded.classrooms`.
Spring HATEOAS derives the rel from the model class name, so `KidModel` emits `_embedded.kidModelList`.
**Every `*Model` needs `@Relation(collectionRelation = "kids", itemRelation = "kid")`.** An ArchUnit
rule (§7.2) turns this from a Cucumber-only failure into a fast one.

`PagedModel`'s `page: {size, totalElements, totalPages, number}` block comes from
`PagedResourcesAssembler` (Spring Data Web, not Spring Data REST) and survives unchanged.

**Verify in Phase 0:** Boot 4.1 / Jackson 3 (`tools.jackson`, `jackson-databind` 3.1.5) — confirm
`HypermediaAutoConfiguration` still makes HAL the default JSON representation without an explicit
`@EnableHypermediaSupport(type = HAL)`. No test asserts the content type, but the `_links`/`_embedded`
*structure* is asserted everywhere.

### 5.6 Base path `/v0` and link generation — **spike this first**

`spring.data.rest.base-path=v@parsedVersion.majorVersion@` is Maven-filtered from
`build-helper:parse-version`, and `SpringSecurityConfiguration` reads it back out of
`RepositoryRestConfiguration`. Both ends need replacing.

Risk: `WebMvcLinkBuilder.linkTo(methodOn(…))` builds from `@RequestMapping` *annotation values*, so a
prefix added via `PathMatchConfigurer.addPathPrefix(…)` may not reach generated `self` links — and
`kid/read.feature`, `school/read.feature`, `classroom/bind/teacher.feature` all assert
`selfLink.endsWith("/v0/…")`. Ordered preference:

1. `@RequestMapping("${lyra.api.base-path}/kids")` — verify Spring HATEOAS resolves the placeholder too.
2. `addPathPrefix` + verify link output.
3. Fallback (always works): the adapter builds links from `ApiBasePath` via `UriComponentsBuilder`.

### 5.7 Controller stereotype vs. the explicit-`@Bean` convention

`SpringBeanRulesTest.sliceBeansAreNotComponentScanned` bans `@Component`/`@Service` outside `config`.
A plain `@RestController` is meta-annotated `@Component`, so it would be component-scanned — and if
*also* declared as a `@Bean` you get two beans and an ambiguous-mapping failure at startup.

**Decided:** class-level `@RequestMapping` + `@ResponseBody`, **no stereotype**, registered as a
`@Bean`. `RequestMappingHandlerMapping.isHandler()` accepts a type annotated with `@Controller`
*or* `@RequestMapping`, so this works and preserves the explicit-wiring philosophy. Verify on Spring
Framework 7 in the Phase 0 spike; fallback is `@RestController` + component scanning for controllers only.

Related: **never use MapStruct `componentModel = "spring"`** — it emits `@Component` on the generated
`*MapperImpl`, which ArchUnit sees in `target/classes`. The existing `sliceBeansAreNotComponentScanned`
rule already catches this, which is a nice free guard rail. Use the default component model and expose
each mapper via `Mappers.getMapper(X.class)` in a `@Bean` method.

### 5.8 Optimistic locking / ETag

**Decided: do not reimplement.** Spring Data REST emits `ETag` from `@Version` and honours `If-Match`;
nothing tests it and no known client uses it. Note the loss in the README; revisit if a client asks.

### 5.9 Transaction boundaries

`@Transactional` currently sits on repositories, so every `save()` is its own transaction. Adapter-level
`@Transactional` moves the flush to the adapter boundary — still inside the request, so
`ProblemDetailsControllerAdvice` keeps catching `DataIntegrityViolationException` → 409. Timing
changes, so re-run the duplicate-creation scenarios attentively.

---

## 6. Build changes (`pom.xml`)

```diff
- spring-boot-starter-data-rest
+ spring-boot-starter-hateoas
+ org.mapstruct:mapstruct
```

`spring-boot-starter-hateoas` pulls in `spring-boot-starter-web` transitively.

- Add `mapstruct-processor` to `maven-compiler-plugin/annotationProcessorPaths` **after** `lombok`,
  plus `lombok-mapstruct-binding` — without it Lombok-generated accessors are invisible to MapStruct
  and every mapper silently maps nothing.
- `application.properties`: drop `spring.data.rest.base-path`; add `lyra.api.base-path` (keeping the
  `@parsedVersion.majorVersion@` filter) and `spring.data.web.pageable.default-page-size=20` to match
  Spring Data REST's default.
- **Quality gates:** `sonar.qualitygate.wait=true` is on and pitest targets `edu.lyra.members.api.*`,
  excluding only `**.*Configuration` / `**.*Application`. MapStruct's generated `*MapperImpl` classes
  would otherwise count for coverage and mutation; `javax.annotation.processing.Generated` is
  `SOURCE`-retention, so JaCoCo's auto-ignore does **not** apply. Add `**/*MapperImpl` to
  `sonar.coverage.exclusions` and to the pitest `excludedClasses`.
- **`maven-enforcer-plugin`** (version managed by `spring-boot-dependencies`, no explicit executions
  inherited): add a `bannedDependencies` rule at `validate` excluding
  `spring-boot-starter-data-rest`/`spring-data-rest-core`/`spring-data-rest-webmvc`/
  `spring-data-rest-hal-explorer` with `searchTransitive` on, so the dependency cannot return even
  transitively (§7.11).

---

## 7. Architecture tests (ArchUnit)

The suite is deeply coupled to Spring Data REST and will fail hard on day one. Below is the complete
change set: **14 existing files** (7 unchanged, 7 rewritten or extended) plus **3 new files**.
`checkstyle-architecture.xml` requires Javadoc on every `@ArchTest` field, so each new rule needs the
house Compliant/Violation Javadoc block.

Unchanged: `ApplicationRulesTest`, `CodingRulesTest`, `ExceptionRulesTest`, `GeneralRulesTest`,
`SpringBeanRulesTest`, `TestSuiteRulesTest`, `SecurityRulesTest` (extended below, not rewritten).

### 7.1 `WebRulesTest` — rewritten

| Rule | Fate |
|---|---|
| `noPlainRestControllers` | **kept, new rationale**: still forbids `@RestController`/`@Controller`, now because controllers are `@RequestMapping`-only `@Bean`s (§5.7), not because of Spring Data REST |
| `mappedControllerMethodsAreNotPublic` | **retargeted** at type-level `@RequestMapping` classes |
| `handlerMethodsArePublic` | **deleted** — `@RepositoryEventHandler` is gone |
| *new* `controllersDoNotDependOnRepositories` | the core decoupling rule: `noClasses().that().haveSimpleNameEndingWith("Controller").should().dependOnClassesThat().areAssignableTo(Repository.class)` |
| *new* `controllersDoNotDependOnEntities` | enforces the DTO boundary — no `@Entity` type in a controller |
| *new* `controllersDoNotDependOnPersistence` | no `jakarta.persistence..` in a controller |
| *new* `controllersAreNotTransactional` | transactions belong to the adapter, not the HTTP edge |
| *new* `controllersDoNotBuildProblemDetails` | error shaping stays centralised in `ProblemDetailsControllerAdvice` |

### 7.2 `RepresentationRulesTest` — **new file**

Guards the inbound/outbound asymmetry of §3.2 and the `@Relation` trap of §5.5.

| Rule | Intent |
|---|---|
| `responseModelsExtendRepresentationModel` | `*Model` must be assignable to `org.springframework.hateoas.RepresentationModel` |
| `responseModelsDeclareTheirRelation` | `*Model` must be annotated `@Relation` — **turns a Cucumber-only `_embedded.kidModelList` failure into a fast unit failure** |
| `requestDtosAreRecords` | `*Request` must be records |
| `requestDtosDoNotDependOnHateoas` | `*Request` must not depend on `org.springframework.hateoas..` — inbound payloads carry no links |
| `representationsDoNotDependOnEntitiesOrRepositories` | neither `*Model` nor `*Request` may reference an `@Entity` or a `Repository` |
| `representationsLiveInRestPackages` | both reside in `..rest` |
| `representationsDoNotDependOnPersistence` | no `jakarta.persistence..` |

### 7.3 `AdapterRulesTest` — **new file**

Several rules originally sketched here turned out to duplicate the bidirectional name/stereotype pairs
already in `NamingRulesTest` (§7.6) — `*Adapter`/`*Mapper`/`*Policy` naming and package location are
asserted there instead, once per type, not twice. `onlyAdaptersDependOnRepositories` was dropped
outright: `TeacherPolicy` and the `*VisibilityStrategy` classes are legitimate non-Adapter repository
consumers (§7.4 covers the real boundary — Controller/Model/Request/Mapper never touch a repository —
from the negative direction instead). `mappersDoNotUseSpringComponentModel` was dropped too: it
duplicated `SpringBeanRulesTest.sliceBeansAreNotComponentScanned`, which already forbids
component-scanned beans (including a MapStruct `componentModel = "spring"` mapper's generated
`@Component` impl) outside `config`. What's left is genuinely new ground:

| Rule | Intent |
|---|---|
| `mappersArePure` | `*Mapper` must not depend on repositories, `AuthenticatedPrincipal`, or `org.springframework.security..` (§3.3) |
| `accessDeniedIsThrownOnlyByPoliciesOrTheSecurityKernel` | `AccessDeniedException` is thrown only from a `*Policy`, or from `AuthenticatedPrincipal` itself in `config.security` (the pre-authorization check every self-service write depends on) — one place owns 403 |

### 7.4 `JpaRepositoryRulesTest` — extended

Existing `repositoriesAreTransactional` and `noJakartaTransactional` stay.

| *new* rule | Intent |
|---|---|
| `repositoriesAreOnlyAccessedByAdapters` | repositories are reachable from `*Adapter` classes and other repositories only — the data-access decoupling the brief asks for, expressed without inventing a service layer (§3.4) |
| `repositoriesDoNotDependOnWeb` | no `..rest` or `org.springframework.web..` dependency from a repository |

### 7.5 `JpaEntityRulesTest` — extended, one rule relaxed

| Rule | Fate |
|---|---|
| `jpaEntitiesAreAnnotatedWithEntityListeners`, `jpaEntitiesUseLombok`, `jpaEntitiesDeclareAllAuditingFields`, `jpaEntitiesExtendAuditable` | unchanged |
| `jpaEntitiesHaveUuidIdField` | **relaxed**: drop the `@JsonIgnore` requirement from `ID_FIELD_ANNOTATIONS`; keep `@Id`, `@Column`, UUID type. Superseded by the stronger rule below |
| `jpaEntitiesDoNotDependOnInfrastructure` | **extended** to also forbid `org.springframework.hateoas..` |
| *new* `jpaEntitiesCarryNoJacksonAnnotations` | no `com.fasterxml.jackson..` / `tools.jackson..` annotation anywhere on an entity — DTOs own the wire format (§2.3) |
| *new* `jpaEntitiesCarryNoBeanValidationAnnotations` | no `jakarta.validation..` annotation — validation lives on request DTOs (§5.4) |

These two new rules are what make the §2.3 cleanup stick instead of drifting back.

### 7.6 `NamingRulesTest` — rewritten

Six of eight rules reference Spring Data REST. The house pattern is a bidirectional pair per type
(name ⇒ annotation, annotation ⇒ name); keep it for each new component type.

| Rule | Fate |
|---|---|
| `repositoryRestControllersAreNamedController` / `controllersAreRepositoryRestControllers` / `controllersLiveInRestPackages` | **retargeted** at type-level `@RequestMapping` |
| `repositoryEventHandlersAreNamedHandler` / `handlersAreRepositoryEventHandlers` / `handlersLiveInHandlersPackages` | **deleted** |
| `springDataRepositoriesAreInterfacesNamedRepository`, `repositoriesLiveInTheirAggregateRoot`, `entitiesDeclareAnExplicitTable` | unchanged, minus the `..handlers` reference |
| *new* | `*Adapter` ⇄ implements `RepresentationModelAssembler`, in `..rest` |
| *new* | `*Mapper` ⇄ `@Mapper`, in `..rest` |
| *new* | `*Model` ⇄ extends `RepresentationModel`, in `..rest` |
| *new* | `*Request` ⇄ record, in `..rest` |
| *new* | `*Policy` in `..rest` |

### 7.7 `LoggingRulesTest` — retargeted

- `IS_SPRING_CONTROLLER` predicate: drop `@RepositoryRestController`, add type-level `@RequestMapping`.
- `repositoryEventHandlersLogTheirEvents` → `adaptersAndPoliciesLogTheirDecisions`: every `*Adapter`
  and `*Policy` carries `@Slf4j` and logs at least one line. This preserves the current property that
  every authorization decision is traceable — the handlers all log today.

### 7.8 `VerticalSliceRulesTest` — simplified

- `kernelPackagesDoNotDependOnVerticalPackages`: **drop the `RepositoryRestConfigurer` exemption.**
  It exists only for `RestExposureConfiguration`, which is deleted, so `config` becomes genuinely
  feature-free — a strictly stronger rule for free.
- Remove `..handlers` from `handlersAndRestPackagesContainNoPublicClasses`,
  `handlersAndRestPackagesAreOnlyAccessedWithinTheirOwnAggregate` and `INTERNAL_PACKAGE_SUFFIX_*`;
  rename both rules to the `..rest`-only form.

### 7.9 `PersonRulesTest` — updated

`REGISTRATION_HANDLERS` names `parent.handlers.ParentRegistrationHandler` and
`teacher.handlers.TeacherRegistrationHandler`, neither of which will exist. Re-point at
`parent.rest.ParentAdapter` and `teacher.rest.TeacherAdapter`. The rule fails loudly rather than
silently if this is forgotten, which is the right failure mode.

### 7.10 `SecurityRulesTest` — extended

Existing two rules unchanged.

| *new* rule | Intent |
|---|---|
| `preAuthorizeOnlyInRestPackages` | `@PreAuthorize` appears only in `..rest` — authorization stays at the boundary, next to the `SecurityFilterChain` matchers |

### 7.11 Guarding against Spring Data REST creeping back in — **not an ArchUnit rule**

An ArchUnit rule here (`noClasses().should().dependOnClassesThat().resideInAPackage(...)`) only catches
the framework returning if some class ends up importing it. A `maven-enforcer-plugin` `bannedDependencies`
rule (§6) is strictly stronger: it fails at `validate`, before compilation, and fires even if the
dependency is merely pulled onto the classpath (directly or transitively) with nothing ever importing
it — which is exactly how `spring-boot-starter-data-rest`'s autoconfiguration could silently reactivate
without a single line of code referencing it. Paired with a Checkstyle `IllegalImport` (§8), so it fails
at two independent gates: one on the dependency tree, one on the source.

### 7.12 Considered and rejected

- **`slices().…should().beFreeOfCycles()`** — attractive and normally best practice, but `Parent` holds
  `Set<Kid>` while `Kid` holds `Parent`, and `Classroom`/`Teacher`/`School` are similarly entangled by
  design. The rule would fail on day one for reasons this migration does not cause and should not try
  to fix. Revisit as a separate piece of work.
- **Repositories as package-private** — would enforce §3.4 at compile time, but breaks the legitimate
  cross-slice use in `PersonRoleAdapter` and would force a service layer for one call site.

---

## 8. Checkstyle

`checkstyle.xml` deliberately owns "the lexical/formatting layer ArchUnit cannot see", so changes stay
lexical. Everything below is additive — no existing module is removed or loosened.

### 8.1 `checkstyle.xml`

| Module | Why now |
|---|---|
| **`IllegalImport`** with `illegalPkgs="org.springframework.data.rest"` | the second, independent gate against Spring Data REST returning (§7.11). Fails with a clear message at `validate` time, before tests run |
| **`RecordComponentName`** | the codebase has **no records today**; request DTOs introduce them and no naming check currently covers their components |
| **`RecordTypeParameterName`** | same reason, completes the record naming set |
| **`UnusedLocalVariable`** | adapters and mappers add a lot of intermediate locals; cheap hygiene, no expected churn on existing code |

### 8.2 `checkstyle.xml` — proposed with a caveat

| Module | Caveat |
|---|---|
| **`ClassFanOutComplexity`** (`max=25`) | Not currently enabled. Adapters legitimately collaborate with a repository, a mapper, a policy, a resolver and several models, so this is exactly the class that can quietly become a god object. A generous ceiling catches that without churn — but **measure against the finished Phase 2 slice before fixing the number**, and do not enable it before then |
| **`OverloadMethodsDeclarationOrder`** | Good practice, but `PersonRoleController` and `TeacherSchoolMembership` already use overloads; verify against main sources before enabling |

### 8.3 `checkstyle-architecture.xml`

Mechanism unchanged (`JavadocVariable`, `accessModifiers=package`, scoped to the architecture package
via its own execution). No config edit is needed — but note that it now applies to **~20 new
`@ArchTest` fields across 3 new files**, each of which needs the house Javadoc block with Compliant
and Violation examples. Budget for it; it is a meaningful share of §7's cost.

### 8.4 Rejected

- **Per-package Checkstyle executions** (e.g. banning `jakarta.persistence` imports inside `**/rest/**`
  via a third config + `includes`): ArchUnit expresses this better and already does (§7.1, §7.2).
  Two mechanisms for one rule is the opposite of the simplicity guideline.
- **`MissingJavadocType` widening to package scope**: every new `..rest` class is package-private by
  design, so widening the scope would demand Javadoc on ~40 new classes for no reader benefit.

---

## 9. Unit tests

Currently 27 unit-test files (~3 400 lines) outside the Cucumber, architecture and `*IT` trees.
Because pitest targets `edu.lyra.members.api.*` and `sonar.qualitygate.wait=true` is on, new adapters
and controllers **must** arrive with unit tests or the build fails on mutation score — this list is
not optional polish.

### 9.1 Removed (1 file)

| File | Why |
|---|---|
| `kid/KidsAssociationMethodsTest` | A `@SpringBootTest` that walks Spring Data REST's `ResourceMappings`/`RepositoryRestConfiguration` metadata to prove every `Kid` association endpoint rejects POST/PUT/PATCH. The metadata API disappears with the framework. **Replaced by** `kid/rest/KidAssociationRoutesTest` (§9.3) asserting the same guarantee through HTTP status codes instead of framework internals — a better test besides |

### 9.2 Updated (18 files)

**Direct Spring Data REST coupling (4):**

| File | Change |
|---|---|
| `config/security/SpringSecurityConfigurationTest` | autowires `RepositoryRestConfiguration` for the base path → `ApiBasePath`; **add** a case per new association path asserting the required authority (§5.2.1) |
| `config/web/ProblemDetailsControllerAdviceTest` | `testRepositoryConstraintViolationErrorResponse` → `MethodArgumentNotValidException`; **add** cases for `HttpMessageNotReadableException` (malformed JSON → 400) and unknown association id → 400 |
| `person/rest/PersonUpdateControllerTest` | drop `verify(eventPublisher).publishEvent(any(BeforeSaveEvent.class))` / `AfterSaveEvent`; assert the policy was consulted instead |
| `person/rest/PersonRoleControllerTest` | drop the `@Qualifier("defaultConversionService") ConversionService` mock and the `Map<String, Object>` body entirely — the endpoint takes a request record with a `@NotNull UUID school` (§5.1) |

**The 14 handler tests → policy/adapter tests.** These are plain JUnit + Mockito + Instancio with
`SecurityContextHolder` set up by hand; the assertions (`AccessDeniedException`, `ParentHasKidsException`,
`SchoolMismatchException`, …) are about *behaviour*, not about Spring Data REST, so they port cheaply:

| From | To | Extra work |
|---|---|---|
| `school/handlers/SchoolUpdateAuthorizationEventHandlerTest` | `school/rest/SchoolPolicyTest` | rename only |
| `school/handlers/SchoolDeleteEventHandlerTest` | `school/rest/SchoolPolicyTest` | merge into one policy test |
| `teacher/handlers/TeacherUpdateAuthorizationEventHandlerTest` | `teacher/rest/TeacherPolicyTest` | rename only |
| `teacher/handlers/TeacherDeleteEventHandlerTest` | `teacher/rest/TeacherPolicyTest` | rename only |
| `teacher/handlers/TeacherRegistrationHandlerTest` | `teacher/rest/TeacherAdapterTest` | becomes an adapter registration test |
| `parent/handlers/ParentUpdateAuthorizationEventHandlerTest` | `parent/rest/ParentPolicyTest` | `authorizeKidBinding(parent, Object linked)` loses the untyped `Object` + `@SuppressWarnings("unchecked")` — signature becomes `(Parent, Collection<Kid>)` |
| `parent/handlers/ParentDeleteEventHandlerTest` | `parent/rest/ParentPolicyTest` | rename only |
| `parent/handlers/ParentRegistrationHandlerTest` | `parent/rest/ParentAdapterTest` | becomes an adapter registration test |
| `kid/handlers/KidUpdateAuthorizationEventHandlerTest` | `kid/rest/KidPolicyTest` | **signature change**: `previousParentId`/`previousClassroomId` are deleted (§2.3), so the policy takes `(kid, newParent, newClassroom)` explicitly. The largest port at 204 lines, and the one to review most carefully |
| `kid/handlers/KidDeleteAuthorizationEventHandlerTest` | `kid/rest/KidPolicyTest` | rename only |
| `classroom/handlers/ClassroomUpdateAuthorizationEventHandlerTest` | `classroom/rest/ClassroomPolicyTest` | **signature change**: `previousTutorId` deleted → policy takes the current tutor explicitly |
| `classroom/handlers/ClassroomDeleteEventHandlerTest` | `classroom/rest/ClassroomPolicyTest` | rename only |
| `classroom/handlers/ClassroomTeacherAssignmentEventHandlerTest` | `classroom/rest/ClassroomAdapterTest` | invariant check moves to the adapter |

**Unchanged in substance (2):** `kid/rest/ParentKidVisibilityStrategyTest` and
`TeacherKidVisibilityStrategyTest` — the visibility strategies survive the migration untouched.

**Untouched entirely (8):** the remaining `config/security/*` tests and `JpaAuditingTest` are
framework-agnostic.

### 9.3 Added (~26 files)

**Shared web infrastructure (2):**

| Test | Cases |
|---|---|
| `ApiBasePathTest` | property binding from `lyra.api.base-path`; prefix appears in a built link |
| `RootControllerTest` | `GET /v0/` returns a link per collection |

§5.1 removes the two largest infrastructure tests before they are ever written — there is no
`text/uri-list` converter and no URI resolver to test.

**Per aggregate × 6 (Parent, Kid, School, Teacher, Classroom, Person) — 18 files:**

| Test | Cases |
|---|---|
| `<X>MapperTest` | request → entity; entity → model (fields only, no links); patch with `@MappingTarget` leaves absent fields untouched; null handling. Fast, no Spring context |
| `<X>AdapterTest` | 404 when the entity is absent; **policy consulted before any mutation** (the §2.2 ordering contract, pinned at unit level rather than only in Cucumber); mapper invoked; `self` link present and `/v0`-prefixed; `@Relation` rel on collections |
| `<X>ControllerTest` | status codes and headers in isolation with a mocked adapter: 201 + `Location`, 204, 400, 403, 404, 409, 422; association `PUT`s are idempotent (twice → 204 both times); unknown owner **or** unknown target id → 404 |

**Route-shape guards (2):**

| Test | Cases |
|---|---|
| `kid/rest/KidAssociationRoutesTest` | replaces `KidsAssociationMethodsTest`: POST/PUT/PATCH on `/v0/kids/{id}/parent` and `/v0/kids/{id}/classroom` → 405; GET → 200. Same guarantee, no framework internals |
| `config/web/DisabledMethodsTest` | the item-level `PUT`s that `RestExposureConfiguration` disables today (parents, kids, teachers, schools, classrooms) and `POST /v0/persons` → 405, so the exposure rules survive as explicit assertions rather than vanishing with their configuration class |

`DisabledMethodsTest` is worth calling out: `RestExposureConfiguration` is currently the *only*
statement that those methods are closed, and no scenario covers them. Deleting it without this test
would silently widen the API.

### 9.4 Not changed

Cucumber step definitions, `ScenarioContext`, `EntityFixtures`, `TestSecurityContext` and all four
`*IT` tests stay as they are — they speak raw HTTP and reach the database through repositories, both
of which survive. Any diff there during Phases 0–4 is a signal that the contract moved.

---

## 10. Phased execution

Each phase ends green. Cucumber is the gate throughout.

**Phase −1 — live security fixes, on `main`, before anything else (½ day).** Independent of the
migration and reviewable on its own: add the `POST /v0/classrooms` matcher (F1), add the seven missing
scopes to `lyra-realm.json` (F2), narrow `/actuator/**` to health and info (F5), and add an `*IT`
delete so F2 stays fixed. Do **not** bury these in a 70-class refactor.

**Phase 0 — spikes (½ day).** One hard-coded `@RequestMapping` endpoint returning
`PagedModel<SchoolModel>` answering four questions: §5.5 (HAL default under Boot 4.1 / Jackson 3, and
`@Relation` drives `_embedded.schools`), §5.6 (the `/v0` prefix reaches generated `self` links),
§5.7 (a `@RequestMapping`-only `@Bean` is picked up as a handler), and §3.2 (whether MapStruct flags
`links` on a `RepresentationModel` subclass). None is worth discovering in Phase 3.

**Phase 1 — infrastructure, no behaviour change (½–1 day).** Swap the starters; add MapStruct and its
processor path; add `ApiBasePath` with its unit test; re-point `SpringSecurityConfiguration` and
`SpringSecurityConfigurationTest` off `RepositoryRestConfiguration`. Spring Data REST is still
serving — nothing breaks yet. Shorter than previously estimated because §5.1 removed two of the three
infrastructure components.

**Phase 2 — walking skeleton on School (1–2 days).** Smallest surface: 19 scenarios, no
association sub-resources, no `Person` delegation, no visibility rules. Build controller, adapter, mapper, model,
requests, policy and slice config, plus the three new test classes, and make `school/*.feature` pass
with `RestExposureConfiguration` still shadowing the generated routes. Then **measure the adapter's
fan-out and fix the `ClassFanOutComplexity` ceiling** (§8.2). This phase decides the pattern.

**Phase 3 — remaining slices (5–7 days), hardest last.**
1. **Teacher** — association-by-URI (`school`), `Person` delegation; **8 Gherkin lines** (§5.4).
2. **Parent** — `Person` delegation, the kid-binding sub-resource, created-by check; **8 Gherkin lines**.
3. **Classroom** — school-mismatch invariant (422), three association sub-resources (tutor, teachers,
   kids) and the new security matchers they need (§5.2.1).
4. **Kid** — visibility strategies, URI-based re-parenting and enrolment, the most intricate policy
   (`kid/update.feature` alone has 11 scenarios); `KidPolicyTest` is the largest port.
5. **Person** — mostly existing code; convert the controllers and drop the `BeforeSaveEvent`/
   `AfterSaveEvent` publishing.

**Phase 4 — remove Spring Data REST (1½–2 days).** Switch the chain to `.anyRequest().denyAll()` with
the `DispatcherType.ERROR` allowance (§11.4) and land `EndpointCoverageTest` — the route table is
final at this point, so this is where fail-closed becomes permanent. Delete the starter, both
`RepositoryRestConfigurer`s and all `*/handlers/**`. Add `DisabledMethodsTest` and
`KidAssociationRoutesTest` **before** deleting `RestExposureConfiguration` and
`KidsAssociationMethodsTest`, so the guarantees never lapse. Strip
Jackson and Bean Validation annotations and the `previous*Id` transients from the entities (§2.3).
Land the ArchUnit rewrite (§7) and the Checkstyle additions (§8), plus the `maven-enforcer-plugin`
`bannedDependencies` rule (§6, §7.11) and the `IllegalImport`, which are what stop it coming back.

**Total: ~10–12 working days** — Phase −1 (½ day) plus ~9–11 for the migration itself, of which
roughly a quarter is the rules-and-tests work in §7–§9. §5.1 is close to effort-neutral: it deletes
two infrastructure classes and their tests, and adds 11 step-definition edits plus new matchers.
The §11 security work adds about a day, most of it filling out `SpringSecurityConfigurationTest` to
one allow/deny pair per route.

---

## 11. Security — every endpoint, every scope

Treated as P0. The audit below covers the **complete** endpoint surface of §4, not just what changes.
Three of the five findings are live in `main` today and are not caused by this migration.

### 11.1 Findings

**F1 — `POST /v0/classrooms` is not scope-gated. (live)**
`SpringSecurityConfiguration` has `POST` matchers for parents, kids, schools and teachers — **not for
classrooms**. The request falls through to `.anyRequest().authenticated()`, so **any authenticated
token can create a classroom**. `classroom/create.feature` already declares the intent
(`Given I am authenticated with "classrooms.create" scope`) but the chain never enforces it: the
scenario passes because the scope happens to be present, and a token *without* it would succeed
identically. The README's scope table records Classrooms create as "—", which the feature file
contradicts.

**F2 — no `*.delete` scope exists in the identity provider. (live)**
`src/test/resources/keycloak/lyra-realm.json` defines exactly 14 scopes, and **not one is a delete
scope**; `classrooms.create` and `persons.read` are absent too. The chain demands `parents.delete`,
`kids.delete`, `schools.delete`, `teachers.delete` and `classrooms.delete`, so **all five DELETE
endpoints are unreachable against the real IdP** — every request 403s. Neither suite catches it:
Cucumber mints JWTs with arbitrary authorities via `jwt().authorities(…)`, and no `*IT` exercises a
delete.

**F3 — association reads leak across scopes. (live)**
`GET /v0/classrooms/{id}/teachers` and `…/tutor` require only `classrooms.read` yet return teacher
representations, so `classrooms.read` is a backdoor to the teacher directory. Each association GET
being reinstated in §4 adds another instance — `/parents/{id}/kids` returns kid data under
`parents.read`, `/schools/{id}/teachers` returns teacher data under `schools.read`, and so on.

**F4 — the chain fails open. (structural)**
`.anyRequest().authenticated()` means any route nobody remembered to enumerate is reachable by any
valid token. F1 is an instance of it, and so was the `PUT /classrooms/{id}/teachers/{teacherId}` hole
in §5.2.1. Every future endpoint inherits the same failure mode.

**F5 — `/actuator/**` is entirely `permitAll`.** Only the health probes and `info` need to be
anonymous; everything else the actuator may expose should not be.

### 11.2 The rules the matrix follows

Stated once so the table is derivable rather than memorised:

1. **Write** on aggregate X → `SCOPE_x.<create|update|delete>`.
2. **Read** returning X's representation → `SCOPE_x.read`.
3. **Association read** returning Y's representation under X's path → **both** `x.read` **and**
   `y.read`. This is what closes F3.
4. **Association write** on X referencing Y by id → `x.update` **only**. The response is 204 with no
   body, so nothing about Y is disclosed; requiring `y.read` would add ceremony to close only a weak
   existence oracle (400 vs 204). Recorded deliberately rather than by omission.
5. **Record-level entitlement** (admin / parent / teacher) stays *out* of the chain — it depends on
   the record, not the route, and is enforced in `*Policy` via `AuthenticatedPrincipal`. The chain
   answers "may this token touch this kind of resource at all"; the policy answers "may this caller
   touch *this* one".
6. **Default deny**, so a missing rule fails closed.

### 11.3 The complete matrix

Rows marked **new** did not exist or were not enforced before.

| Method | Path | Required authority |
|---|---|---|
| GET | `/actuator/health/**`, `/actuator/info` | *anonymous* |
| any | `/actuator/**` (anything else) | `denyAll` — **new** (F5) |
| — | `DispatcherType.ERROR` | *permit* — **new** (§11.4) |
| GET | `/v0/` | authenticated — **new** (root index) |
| GET | `/v0/parents`, `/v0/parents/{id}` | `parents.read` |
| GET | `/v0/parents/{id}/kids` | `parents.read` **+ `kids.read`** — **new** (F3) |
| POST | `/v0/parents` | `parents.create` |
| PATCH | `/v0/parents/{id}` | `parents.update` |
| DELETE | `/v0/parents/{id}` | `parents.delete` |
| PUT | `/v0/parents/{id}/kids/{kidId}` | `parents.update` — **new** (§5.2.1) |
| GET | `/v0/kids`, `/v0/kids/{id}` | `kids.read` |
| GET | `/v0/kids/{id}/parent` | `kids.read` **+ `parents.read`** — **new** |
| GET | `/v0/kids/{id}/classroom` | `kids.read` **+ `classrooms.read`** — **new** |
| POST | `/v0/kids` | `kids.create` |
| PATCH | `/v0/kids/{id}` | `kids.update` |
| DELETE | `/v0/kids/{id}` | `kids.delete` |
| GET | `/v0/schools`, `/v0/schools/{id}` | `schools.read` |
| GET | `/v0/schools/{id}/classrooms` | `schools.read` **+ `classrooms.read`** — **new** |
| GET | `/v0/schools/{id}/teachers` | `schools.read` **+ `teachers.read`** — **new** |
| POST | `/v0/schools` | `schools.create` |
| PATCH | `/v0/schools/{id}` | `schools.update` |
| DELETE | `/v0/schools/{id}` | `schools.delete` |
| GET | `/v0/teachers`, `/v0/teachers/{id}` | `teachers.read` |
| GET | `/v0/teachers/{id}/school` | `teachers.read` **+ `schools.read`** — **new** |
| POST | `/v0/teachers` | `teachers.create` |
| PATCH | `/v0/teachers/{id}` | `teachers.update` |
| DELETE | `/v0/teachers/{id}` | `teachers.delete` |
| GET | `/v0/classrooms`, `/v0/classrooms/{id}` | `classrooms.read` |
| GET | `/v0/classrooms/{id}/school` | `classrooms.read` **+ `schools.read`** — **new** |
| GET | `/v0/classrooms/{id}/teachers` | `classrooms.read` **+ `teachers.read`** — **new** (F3) |
| GET | `/v0/classrooms/{id}/tutor` | `classrooms.read` **+ `teachers.read`** — **new** (F3) |
| POST | `/v0/classrooms` | **`classrooms.create`** — **new** (F1) |
| PATCH | `/v0/classrooms/{id}` | `classrooms.update` |
| DELETE | `/v0/classrooms/{id}` | `classrooms.delete` |
| PUT | `/v0/classrooms/{id}/tutor/{teacherId}` | `classrooms.update` — **new path**; the existing `PUT /classrooms/*/tutor` rule no longer matches (§5.2.1) |
| PUT | `/v0/classrooms/{id}/teachers/{teacherId}` | `classrooms.update` — **new** (§5.2.1) |
| PUT | `/v0/classrooms/{id}/kids/{kidId}` | `classrooms.update` — **new** (§5.2.1) |
| GET | `/v0/persons`, `/v0/persons/{id}` | **`persons.read`** + `ROLE_admin` — **new scope** |
| PUT / DELETE | `/v0/persons/{id}/parent` | `parents.create` + `ROLE_admin` |
| PUT / DELETE | `/v0/persons/{id}/teacher` | `teachers.create` + `ROLE_admin` |
| **any** | **anything else** | **`denyAll`** — **new** (F4) |

The `ROLE_admin` half of the `persons` rows stays where it already is, as `@PreAuthorize` on the
controller; the chain owns the scope. Two layers, each in its natural place, neither duplicated.

### 11.4 Chain construction notes

- **`.anyRequest().denyAll()`** replaces `.authenticated()`. This is the single highest-value change:
  it converts F1, F4 and the §5.2.1 hole from silent holes into startup-visible 403s.
- **`.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` must come first.** Boot's default
  `spring.security.filter.dispatcher-types` is `REQUEST, ASYNC, ERROR`, so under `denyAll` the
  `/error` dispatch would itself be denied and every 4xx would render as an empty 403 instead of the
  intended ProblemDetail. This bites hard and is easy to misdiagnose.
- **AND-composition:** there is no `hasAllAuthorities`. Use
  `.access(AuthorizationManagers.allOf(AuthorityAuthorizationManager.hasAuthority("SCOPE_classrooms.read"),
  AuthorityAuthorizationManager.hasAuthority("SCOPE_teachers.read")))`.
- **Specific before wildcard**, since matching is first-match-wins: `/classrooms/*/teachers/*`,
  `/classrooms/*/tutor/*` and `/classrooms/*/kids/*` must precede `/classrooms/**`. The same ordering
  constraint is what makes a future association `DELETE` demand `classrooms.delete` if registered
  carelessly (§5.2.1).
- **Delete the orphaned `PUT /classrooms/*/tutor` rule** rather than leaving it in place. It matches
  nothing once the path gains a segment, and a matcher that protects nothing while appearing to is a
  review hazard.
- The base path comes from `ApiBasePath`, not `RepositoryRestConfiguration` (§5.6).
- The existing `path(...)`/`scope(...)` helpers extend naturally; add `ANY_SEGMENT`-based constants
  for the new two-segment association paths.

### 11.5 Identity-provider changes (fixes F2)

`src/test/resources/keycloak/lyra-realm.json` needs **seven** new client scopes:
`parents.delete`, `kids.delete`, `schools.delete`, `teachers.delete`, `classrooms.delete`,
`classrooms.create`, `persons.read` — assigned to the test clients that need them. Without this,
every DELETE endpoint stays unreachable in any real deployment and the new `classrooms.create` gate
would lock out legitimate callers.

Worth checking the production realm too: this file is the test fixture, but its shape suggests the
same scopes were never provisioned anywhere.

### 11.6 Test coverage

| Test | Change |
|---|---|
| `SpringSecurityConfigurationTest` | today it covers a handful of creates plus actuator health. Extend to **one allow/deny pair per row** of §11.3. This is the file that would have caught F1 |
| `EndpointCoverageTest` *(new)* | enumerate every `RequestMappingHandlerMapping` entry and assert each is matched by a rule **other than** the `denyAll` fallback. Turns F4 from "hope someone remembers" into a failing build, permanently |
| `*IT` | add a delete for at least one aggregate, so the realm's delete scopes are exercised against real Keycloak. This is the **only** place F2 can be caught — Cucumber cannot, because it fabricates authorities |
| ArchUnit | `preAuthorizeOnlyInRestPackages` (§7.10) keeps role checks at the boundary |

`EndpointCoverageTest` is the structural fix; everything else in this section is a point fix.

### 11.7 Gherkin and step-definition impact

- **F1:** zero — `classroom/create.feature` already grants `classrooms.create`.
- **F3: one line.** `classroom/bind/teacher.feature`'s Background becomes
  `Given I am authenticated with "classrooms.read teachers.read" scope`. It affects 3 scenarios —
  "Get a classroom's teachers", "Get a classroom's tutor", and "Cannot get a classroom's tutor when
  none has been assigned" (whose expected 404 would otherwise become a 403).
- **One step-definition change:** `AuthenticationFeatures.iAmAuthenticatedWithScope` splits its
  argument on whitespace, so `"classrooms.read teachers.read"` grants both. That is how an OAuth2
  scope string is written anyway, so the Gherkin stays readable and no new step is needed. Apply the
  same split to the other three `I am authenticated…` steps for consistency.
- The reinstated association GETs (`/parents/{id}/kids`, `/kids/{id}/parent`, …) have **no scenarios
  at all** today. Add one read scenario each, or at minimum the `SpringSecurityConfigurationTest`
  rows — otherwise they ship untested.

### 11.8 Sequencing

F1, F2 and F5 are **live defects independent of this migration**. Fix them in a **separate commit on
`main` before Phase 1**, so the fix is reviewable on its own and does not arrive buried in a 70-class
refactor. F3, F4 and the new matchers land with the slices that create them, and
`EndpointCoverageTest` lands in Phase 4 once the route table is final.

## 12. Decisions taken

Recorded so the rationale is not re-litigated mid-migration.

| # | Decision | Rationale |
|---|---|---|
| 1 | Hypermedia via **Spring HATEOAS**; `*Model extends RepresentationModel`, `*Request` stays a plain record | maintainer's call; inbound payloads have no links to carry (§3.2) |
| 2 | An **adapter layer** sits between controllers and repositories and owns the translation | maintainer's call; matches the original brief's "MapStruct for adapters" (§3.1) |
| 3 | **Validate request DTOs, not entities**; drop Bean Validation from entities; change 16 Gherkin lines | simplest code; `person.` was a leaked persistence detail (§5.4) |
| 4 | Repositories stay public and cross-slice; decoupling enforced by "controllers never touch repositories" | avoids a service layer that would exist for one call site (§3.4) |
| 5 | Reproduce association GETs; **drop `/v0/profile`**; reinstate `GET /v0/` | ALPS has no consumer here (§4) |
| 9 | **Drop URI-based association references and `text/uri-list`**; expose `id` on models; associations referenced by id; **every** association write is `PUT /{owner}/{id}/{rel}/{targetId}` with no body, single-valued and collection alike | neither is part of HATEOAS — one is an SDR idiom, the other has no standard basis. One uniform shape means one controller, matcher and test pattern instead of two; deletes 2 infrastructure classes, 2 test classes and every association request DTO; zero Gherkin churn (§5.1) |
| 10 | **Breaking API changes are acceptable** | confirmed: there are no clients |
| 11 | **The chain defaults to `denyAll`**; association *reads* need both scopes, association *writes* only the owner's write scope | a missing matcher must fail closed — that is what allowed F1 and the §5.2.1 hole; reads disclose the other aggregate's data, 204 writes do not (§11.2) |
| 12 | **Live security defects are fixed on `main` first**, not inside the migration | F1/F2/F5 predate this work and should be reviewable without a 70-class diff (§11.8) |
| 6 | **No `ETag`/`If-Match`** reimplementation | untested, no known client; documented as a loss (§5.8) |
| 7 | Controllers are `@RequestMapping`-only `@Bean`s, no stereotype | preserves the explicit-wiring convention (§5.7) |
| 8 | **No slice cycle-freedom rule** | the entity graph is cyclic by design; out of scope (§7.12) |
