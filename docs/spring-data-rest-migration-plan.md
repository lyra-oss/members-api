# Migrating off Spring Data REST to plain Spring MVC + Spring Data JPA

Status: **plan / feasibility assessment** — no production code changed yet.

## 1. Verdict

The migration is feasible and low-risk *in terms of behaviour*, because the Gherkin suite
(24 feature files, 163 scenarios, plus 4 Keycloak-backed `*IT` tests) pins the HTTP contract
end-to-end and is written entirely against `MockMvc`/OkHttp — not against Spring Data REST APIs.
That suite is the safety net: it can stay almost entirely untouched and go from green to green.

It is *not* a small job. Spring Data REST is not just generating controllers here; it is silently
providing seven things this codebase depends on:

1. CRUD endpoints for 6 aggregates (~35 routes).
2. HAL rendering — `_embedded.<rel>`, `_links.self`, `page` metadata.
3. **URI-as-foreign-key** in JSON bodies (`"school": "/v0/schools/{id}"`) via `UriToEntityConverter`.
4. **`text/uri-list` association endpoints** (`POST /classrooms/{id}/teachers`, `PUT .../tutor`, …).
5. The **repository event model** (`@HandleBeforeCreate/Save/Delete/LinkSave`) — which is where *all*
   authorization and every business invariant currently lives.
6. Entity validation via `ValidatingRepositoryEventListener`, producing the `errors[]` ProblemDetail shape.
7. The `/v0` base path, wired into `SpringSecurityConfiguration` via `RepositoryRestConfiguration`.

Every one of those has to be rebuilt by hand. The good news is that the codebase is already
half-migrated: `KidsCollectionController`, `PersonUpdateController` and `PersonRoleController` are
hand-written controllers that exist precisely because Spring Data REST got in the way — which is
direct evidence for the premise behind this migration.

Rough size: **~60–75 new/modified main classes**, ~8 ArchUnit rule files rewritten, 1 `pom.xml`
change, ~20 lines of Gherkin touched (see §5.4 for the one contract decision that forces it).

---

## 2. What has to be replaced, precisely

### 2.1 Deleted outright

| Class | Why |
|---|---|
| `config/web/RestExposureConfiguration` | `RepositoryRestConfigurer`; exposure rules become explicit routes |
| `config/web/ValidationConfiguration` | `ValidatingRepositoryEventListener` → explicit validation in the service layer |
| all 14 `*/handlers/*EventHandler` + `*Handler` classes | `@RepositoryEventHandler` disappears; logic moves, does not vanish |
| all 6 `*/handlers/*HandlersConfiguration` | replaced by slice configurations |

`classroom/handlers/TeacherSchoolMembership` is pure domain logic and survives as-is (relocated).

### 2.2 Behaviour that must move, not disappear

This is the crux of the migration — the `handlers` package is the authorization layer:

| Current handler | Becomes |
|---|---|
| `KidAuthorizationEventHandler` (beforeCreate) | `KidService.create` resolves the authenticated parent, 403 if none |
| `KidUpdateAuthorizationEventHandler` (beforeSave) | `KidAccessPolicy.authorizeUpdate(kid, newParent, newClassroom)` |
| `KidDeleteAuthorizationEventHandler` | `KidAccessPolicy.authorizeDelete` |
| `ParentRegistrationHandler` | `ParentService.register` — binds JWT subject → `Person` |
| `ParentUpdateAuthorizationEventHandler` (beforeSave + **beforeLinkSave**) | `ParentAccessPolicy.authorizeUpdate` / `.authorizeKidBinding` |
| `ParentDeleteEventHandler` | `ParentAccessPolicy.authorizeDelete` + `ParentHasKidsException` guard |
| `TeacherRegistrationHandler` / `TeacherUpdate…` / `TeacherDelete…` | `TeacherService` + `TeacherAccessPolicy` |
| `SchoolUpdate…` / `SchoolDelete…` | `SchoolAccessPolicy` |
| `ClassroomUpdateAuthorizationEventHandler` (beforeSave + beforeLinkSave) | `ClassroomAccessPolicy` |
| `ClassroomTeacherAssignmentEventHandler` | `ClassroomService` invariant check on create + staff mutation |

**Order of operations must be preserved exactly**, because the scenarios assert the distinction:
`load → 404 if absent → authorize → 403 → invariant → 409/422 → validate → 400 → persist → 2xx`.
E.g. `kid/update.feature` "Cannot update a kid that does not exist" expects 404, while
"A parent cannot update another parent's kid" expects 403 — so the lookup must precede the
authorization check.

### 2.3 A simplification the migration buys us

`Kid.previousParentId`, `Kid.previousClassroomId` and `Classroom.previousTutorId` are `@Transient`
`@PostLoad` fields that exist *only* because Spring Data REST mutates a loaded entity in place, so
the `beforeSave` handler can no longer see the pre-merge value. With explicit services the old and
new values are both in scope at the call site. **All three fields and both `@PostLoad` methods can
be deleted**, along with the `@JsonIgnore` noise on them.

---

## 3. Target architecture

Layering per vertical slice, honouring the existing `VerticalSliceRulesTest` conventions
(internal sub-packages are package-private and only reachable from their own aggregate):

```
edu.lyra.members.api.<aggregate>/
├── <Aggregate>.java                    JPA entity            (domain, unchanged)
├── <Aggregate>Repository.java          Spring Data JPA       (data access)
├── <Aggregate>Service.java             application service   (public inbound port of the slice)
├── policy/                             ← new internal package (was: handlers/)
│   └── <Aggregate>AccessPolicy.java    authorization + invariants
└── rest/                               ← internal, package-private
    ├── <Aggregate>Controller.java      HTTP mapping only
    ├── <Aggregate>Resource.java        response DTO   @Relation(collectionRelation = "<rel>")
    ├── <Aggregate>CreateRequest.java   request DTO
    ├── <Aggregate>PatchRequest.java    request DTO
    ├── <Aggregate>Mapper.java          MapStruct
    ├── <Aggregate>ModelAssembler.java  HATEOAS links
    └── <Aggregate>RestConfiguration.java  explicit @Bean wiring
```

**Decoupling rules this enforces (per the brief):**

- The controller never touches a repository — only its own `<Aggregate>Service`.
- The repository never leaves the slice: cross-slice access goes service → service.
  (`PersonRoleController` currently reaches into `ParentRepository`, `TeacherRepository` and
  `ClassroomRepository` directly; it becomes `PersonService` → `ParentService`/`TeacherService`.)
- JPA entities never reach the wire: MapStruct maps entity ⇄ DTO at the `rest` boundary.

### 3.1 Shared web infrastructure (`config/web/`)

| New component | Purpose |
|---|---|
| `ApiBasePath` (`@ConfigurationProperties`) | replaces `RepositoryRestConfiguration.getBasePath()`; feeds security config *and* link building |
| `UriListHttpMessageConverter` | Spring MVC has **no** `text/uri-list` support out of the box |
| `EntityUriResolver` | `/v0/schools/{uuid}` → `UUID`; replaces `UriToEntityConverter` and the `@Qualifier("defaultConversionService")` injection |
| `ProblemDetailsControllerAdvice` (modified) | must now handle `MethodArgumentNotValidException` / `ConstraintViolationException` instead of `RepositoryConstraintViolationException` |
| `RootController` (optional) | Spring Data REST serves a `GET /v0/` index of collection links; reimplement to keep HAL discoverability |

---

## 4. Endpoint inventory to reproduce

Derived from `SpringSecurityConfiguration`, the step definitions and the `*IT` tests. ~35 routes.

| Method | Path | Status | Notes |
|---|---|---|---|
| GET | `/v0/parents`, `/v0/parents/{id}` | 200 | paged HAL |
| POST | `/v0/parents` | 201 + `Location` | binds JWT subject to `Person` |
| PATCH | `/v0/parents/{id}` | 204 / 404 | delegating `name`/`surname`/`mail` |
| DELETE | `/v0/parents/{id}` | 204 / 409 | 409 if kids linked |
| POST | `/v0/parents/{id}/kids` | 204 | **`text/uri-list`** |
| GET | `/v0/kids` | 200 | **visibility-filtered** (admin / parent / teacher / none) |
| GET | `/v0/kids/{id}` | 200 / 404 | |
| POST | `/v0/kids` | 201 + `Location` | assigns authenticated parent |
| PATCH | `/v0/kids/{id}` | 204 / 403 / 404 | `parent` + `classroom` given **as URIs** |
| DELETE | `/v0/kids/{id}` | 204 | |
| GET | `/v0/schools`, `/{id}` | 200 | |
| POST/PATCH/DELETE | `/v0/schools[/{id}]` | 201/204/409 | 409 if classrooms or teachers linked |
| GET | `/v0/teachers`, `/{id}` | 200 | |
| POST | `/v0/teachers` | 201 | `school` as URI |
| PATCH/DELETE | `/v0/teachers/{id}` | 204 / 409 | 409 if tutoring/teaching |
| GET | `/v0/classrooms`, `/{id}` | 200 | |
| POST | `/v0/classrooms` | 201 / 422 | `school` + `tutor` as URIs; 422 on school mismatch |
| PATCH/DELETE | `/v0/classrooms/{id}` | 204 / 409 | 409 if kids enrolled |
| GET | `/v0/classrooms/{id}/teachers` | 200 | `_embedded.teachers` |
| POST | `/v0/classrooms/{id}/teachers` | 204 / 404 / 422 | **`text/uri-list`** |
| GET | `/v0/classrooms/{id}/tutor` | 200 / **404 when unset** | |
| PUT | `/v0/classrooms/{id}/tutor` | 204 / 422 | **`text/uri-list`** |
| POST | `/v0/classrooms/{id}/kids` | 204 | **`text/uri-list`** |
| GET | `/v0/persons`, `/{id}` | 200 | admin only |
| PUT/DELETE | `/v0/persons/{id}/{parent,teacher}` | 204 / 400 / 404 / 409 | already hand-written |

Spring Data REST additionally exposes association GETs that no test covers — `/parents/{id}/kids`,
`/kids/{id}/parent`, `/kids/{id}/classroom`, `/teachers/{id}/school`, `/schools/{id}/classrooms`,
`/schools/{id}/teachers`, `/classrooms/{id}/school` — plus `/v0/profile` (ALPS metadata).
**Decision required:** reproduce, or drop and document as a breaking change. Recommendation: keep the
navigable GETs that back `_links` rels we emit; drop `/profile` (ALPS has no consumer here).

---

## 5. The hard parts — compatibility risks and decisions

### 5.1 Association-by-URI in request bodies

`POST /v0/teachers` sends `{"name":…, "school":"/v0/schools/{uuid}"}`; `PATCH /v0/kids/{id}` sends
`{"parent":"/v0/parents/{uuid}"}`. This is `UriToEntityConverter`.

**Plan:** request DTOs declare these as `URI`. MapStruct resolves them through qualified methods
backed by `EntityUriResolver` + the target slice's service:

```java
@Mapper(uses = SchoolService.class)
interface TeacherMapper {
    @Mapping(target = "school", source = "school", qualifiedByName = "schoolFromUri")
    Teacher toEntity(TeacherCreateRequest request);
}
```

Unresolvable URI must yield 400 (`person/roles.feature` — "Cannot make a person a teacher without a
school" expects 400), and *absent* `school` must yield the field error `school` / "must not be null"
(`teacher/create.feature`). Two different codes from two different causes — worth an explicit test.

### 5.2 `text/uri-list`

Five endpoints consume it. Spring MVC ships no converter. Add `UriListHttpMessageConverter` in
`config/web` and declare `consumes = "text/uri-list"`. Body is newline-separated URIs; the tests
always send exactly one, but the converter should return a `List<URI>` and the endpoints should
accept a collection to match Spring Data REST's actual semantics.

### 5.3 PATCH partial-merge semantics

Spring Data REST's `DomainObjectReader` merges only the properties present in the body. Replicating
that with DTOs means distinguishing *absent* from *explicitly null*.

**Plan:** MapStruct `@MappingTarget` +
`@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)`, i.e.
`null` = "not supplied".

**Known narrowing:** this makes it impossible to `PATCH` a field to `null`. Checked against every
scenario — explicit nulls only ever appear on **create** (`parent/create.feature`,
`teacher/create.feature`), never on PATCH — so no scenario regresses. If nulling-out is wanted later,
switch those DTO fields to `JsonNullable<T>`. Document the narrowing in the README.

### 5.4 Validation error paths — the one deliberate contract change

Today the JSR-380 validator runs against the **entity**, so a bad parent/teacher name reports the
path `person.name` (because `PersonRole.setName` delegates into the nested `Person`). School and
classroom report plain `name` / `course` / `group`. The Gherkin asserts both forms:

```gherkin
Then I receive an error stating that "person.surname" field is incorrect because "must not be blank"
Then I receive an error stating that "name" field is incorrect because "must not be blank"
```

`person.*` is a leaked persistence detail — the wire format has no `person` object at all. Two options:

- **(A) Keep entity-level validation.** Validate the mapped entity in the service before save and
  re-emit the same `errors[{entity,property,message}]` array. Contract stays byte-identical, zero
  Gherkin churn, and the whole suite stays a pure regression net.
- **(B) Validate the DTO** with `@Valid @RequestBody`. Paths become `name`/`surname`/`mail`, which
  matches what clients actually send. ~20 Gherkin lines change.

**Recommendation: (A) during the migration, (B) as an immediate follow-up commit.** Doing them
together destroys the ability to tell "the migration broke something" from "we changed the contract
on purpose". Either way `ProblemDetailsControllerAdvice` must grow a
`handleMethodArgumentNotValid`/`ConstraintViolationException` handler emitting the existing shape.

### 5.5 HAL relation names — silent breakage risk

Tests assert `_embedded.kids`, `_embedded.schools`, `_embedded.teachers`, `_embedded.classrooms`.
Spring HATEOAS derives the rel from the DTO class name, so `KidResource` would emit
`_embedded.kidResourceList`. **Every response DTO needs
`@Relation(collectionRelation = "kids", itemRelation = "kid")`.** Cheap, but easy to forget and it
fails loudly only in the Cucumber run.

`PagedModel`'s `page: {size, totalElements, totalPages, number}` block is produced identically by
`PagedResourcesAssembler` (Spring Data Web, not Spring Data REST) — `kid/read.feature` and
`school/read.feature` depend on it and it survives the migration unchanged.

### 5.6 Base path `/v0` and link generation — **spike this first**

`spring.data.rest.base-path=v@parsedVersion.majorVersion@` is Maven-filtered from
`build-helper:parse-version`, and `SpringSecurityConfiguration` reads it back out of
`RepositoryRestConfiguration`. Both ends need replacing.

The risk: `WebMvcLinkBuilder.linkTo(methodOn(…))` builds links from the `@RequestMapping`
*annotation values*, so a prefix added via `PathMatchConfigurer.addPathPrefix(…)` may not appear in
generated `self` links — and `kid/read.feature`, `school/read.feature`, `classroom/bind/teacher.feature`
all assert `selfLink.endsWith("/v0/…")`.

**Plan:** prove this in a one-hour spike before committing to a design. Ordered preference:

1. `@RequestMapping("${lyra.api.base-path}/kids")` — placeholder resolved by MVC; **verify Spring
   HATEOAS resolves it too**.
2. `addPathPrefix` + verify link output.
3. Fallback (always works): assemblers build links from the `ApiBasePath` bean via
   `UriComponentsBuilder` instead of `methodOn`.

### 5.7 Controller stereotype vs. the explicit-`@Bean` convention

`SpringBeanRulesTest.sliceBeansAreNotComponentScanned` bans `@Component`/`@Service` outside `config`;
every slice bean is declared in a `@Configuration`. A plain `@RestController` is meta-annotated
`@Component`, so it would be component-scanned — and if *also* declared as a `@Bean` you get two
beans and an ambiguous-mapping failure at startup.

**Recommendation:** annotate controllers with class-level `@RequestMapping` + `@ResponseBody` and
**no stereotype**, registered explicitly as `@Bean`s. `RequestMappingHandlerMapping.isHandler()`
accepts a type annotated with either `@Controller` *or* `@RequestMapping`, so this works and
preserves the codebase's explicit-wiring philosophy. Verify in the same spike as §5.6 (Spring
Framework 7). Fallback: plain `@RestController` + component scanning, dropping the `@Bean` wiring
for controllers only.

Related: **do not use MapStruct `componentModel = "spring"`** — it emits `@Component` on the
generated `*MapperImpl`, which ArchUnit *will* see in `target/classes` and reject. Use the default
component model and expose each mapper via `Mappers.getMapper(X.class)` in a `@Bean` method.

### 5.8 Optimistic locking / ETag

Spring Data REST emits `ETag` from `@Version` and honours `If-Match` on PATCH/DELETE. Nothing tests
it, but it is a real capability loss. Flag it; reimplement only if a client depends on it.

### 5.9 Transaction boundaries

`@Transactional` currently sits on the repositories, so every `save()` is its own transaction and
constraint violations surface inside the call. Introducing service-level `@Transactional` moves the
flush to the service boundary. Still inside the request, so `ProblemDetailsControllerAdvice` keeps
catching `DataIntegrityViolationException` → 409 (`…/create.feature` duplicate scenarios) — but the
timing changes, so re-run the duplicate-creation scenarios attentively.

---

## 6. Build changes (`pom.xml`)

```diff
- spring-boot-starter-data-rest
+ spring-boot-starter-web
+ spring-boot-starter-hateoas
+ org.mapstruct:mapstruct
```

- Add `mapstruct-processor` to `maven-compiler-plugin/annotationProcessorPaths`
  **after** `lombok`, plus `lombok-mapstruct-binding` — without it Lombok-generated accessors are
  invisible to MapStruct and every mapper silently maps nothing.
- `application.properties`: drop `spring.data.rest.base-path`, add `lyra.api.base-path` (keep the
  `@parsedVersion.majorVersion@` filter) and `spring.data.web.pageable.default-page-size=20` to match
  Spring Data REST's default.
- **Quality gates:** `sonar.qualitygate.wait=true` is on, and pitest targets `edu.lyra.members.api.*`
  excluding only `**.*Configuration` / `**.*Application`. MapStruct's generated `*MapperImpl` classes
  will otherwise be counted for coverage and mutation. `javax.annotation.processing.Generated` is
  `SOURCE`-retention, so JaCoCo's auto-ignore does **not** apply. Add `**/*MapperImpl` to
  `sonar.coverage.exclusions` and to the pitest `excludedClasses`.
- `spring-boot-starter-webmvc-test` is already a test dependency — no test-scope change needed.

---

## 7. ArchUnit rules to rewrite

The architecture suite is deeply coupled to Spring Data REST and will fail hard on day one. This is
a feature, not a problem — but budget for it.

| File | Change |
|---|---|
| `WebRulesTest` | `noPlainRestControllers` **inverts** (currently *forbids* `@RestController`); `mappedControllerMethodsAreNotPublic` re-targeted at the new controller marker; `handlerMethodsArePublic` deleted |
| `NamingRulesTest` | 6 of 8 rules reference `@RepositoryRestController`/`@RepositoryEventHandler`; re-target at the new controller marker and `*Policy`/`*Service` conventions |
| `VerticalSliceRulesTest` | drop the `RepositoryRestConfigurer` exemption in `kernelPackagesDoNotDependOnVerticalPackages` (it exists only for `RestExposureConfiguration`, which is deleted — `config` becomes genuinely feature-free); add `.policy` to the internal-package suffixes |
| `LoggingRulesTest` | `IS_SPRING_CONTROLLER` must recognise the new marker; `repositoryEventHandlersLogTheirEvents` → policies/services |
| `JpaEntityRulesTest` | `jpaEntitiesHaveUuidIdField` requires `@JsonIgnore` on ids — now redundant since DTOs guarantee no leakage. Keep (harmless) or relax deliberately |
| `SpringBeanRulesTest` | unchanged if §5.7's recommendation is followed |
| *new* | repositories may only be accessed from within their own aggregate (enforces §3's data-access decoupling) |
| *new* | JPA entities must not appear in any controller signature (enforces the DTO boundary) |

`checkstyle-architecture.xml` requires Javadoc on every `@ArchTest` field — new rules need it too.

---

## 8. Phased execution

Each phase ends green. Cucumber is the gate throughout.

**Phase 0 — spikes (½ day).** Resolve §5.6 (base path + link generation) and §5.7 (controller
stereotype) on a throwaway branch. Everything downstream depends on these two answers.

**Phase 1 — infrastructure, no behaviour change (1 day).** Swap the starters; add MapStruct and its
processor path; add `ApiBasePath`, `UriListHttpMessageConverter`, `EntityUriResolver`; re-point
`SpringSecurityConfiguration` off `RepositoryRestConfiguration`. Spring Data REST is still on the
classpath and still serving — nothing breaks yet.

**Phase 2 — walking skeleton on one slice (1–2 days).** Migrate **School** first: smallest surface
(4 features, 19 scenarios), no `text/uri-list`, no `Person` delegation, no visibility rules. Build
the full stack — service, policy, DTOs, mapper, assembler, controller, slice config — and make
`school/*.feature` pass with `RestExposureConfiguration` shadowing the generated `/v0/schools`
routes. This proves the pattern end to end, including HAL rels and `page` metadata.

**Phase 3 — remaining slices (4–6 days), hardest last.**
1. **Teacher** — introduces association-by-URI (`school`) and `Person` delegation.
2. **Parent** — `Person` delegation + `text/uri-list` kid binding + the created-by check.
3. **Classroom** — school-mismatch invariant (422), tutor/teachers/kids sub-resources, 3 × `text/uri-list`.
4. **Kid** — visibility strategies, URI-based re-parenting/enrolment, the most intricate
   authorization rules (`kid/update.feature` alone has 11 scenarios).
5. **Person** — mostly existing code; convert `@RepositoryRestController` → the new marker and drop
   the `BeforeSaveEvent`/`AfterSaveEvent` publishing in `PersonUpdateController`.

**Phase 4 — remove Spring Data REST (½ day).** Delete the starter, `RestExposureConfiguration`,
`ValidationConfiguration` and all `*/handlers/**`. Rewrite the ArchUnit suite (§7). Delete the
`previous*Id` transients (§2.3). This is the commit where it can no longer be half-migrated.

**Phase 5 — contract cleanup (½ day, optional).** Apply §5.4 option (B) and update the ~20 Gherkin
lines, as a separate reviewable commit.

**Total: ~8–11 working days.**

---

## 9. Test strategy

- **Gherkin: do not touch** through Phases 0–4, except §5.4-(B) in Phase 5. Any change to a
  `.feature` file during the migration is a red flag — it means the contract moved.
- **Step definitions: do not touch.** They already speak raw HTTP (`get("/v0/kids")`,
  `contentType("text/uri-list")`) and reach the DB through repositories, both of which survive.
- **`*IT` tests: do not touch.** They assert `Location` headers and `_embedded.kids` against a real
  Keycloak — the strongest end-to-end signal available. `KidIT` in particular exercises
  create-parent → create-kid → parent-scoped list.
- **Existing unit tests for handlers** (`*EventHandlerTest`, 13 files) port to the new policy classes
  more or less mechanically — the assertions are about `AccessDeniedException`, not about
  Spring Data REST.
- **New unit tests needed:** mappers (URI ⇄ entity round-trip), `UriListHttpMessageConverter`,
  `EntityUriResolver` (malformed URI → 400), assemblers (rel names + `self` href shape).

---

## 10. Open decisions for you

1. **§5.4** — keep `person.*` validation paths during the migration and clean up after (recommended),
   or change the contract in one go?
2. **§4** — reproduce the untested association GETs and `/v0/profile`, or drop them as a documented
   breaking change?
3. **§5.8** — is `ETag`/`If-Match` optimistic locking worth reimplementing, or is no client using it?
4. **§3** — should repositories become slice-private (new ArchUnit rule), forcing `PersonRoleController`'s
   cross-slice reads through services? Recommended, but it is a wider refactor than the migration
   strictly needs.
