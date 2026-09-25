# members-api

Lyra OSS - Members API

## Configuration

The following properties must be set for the application to start successfully.

### Database

The application requires a PostgreSQL database. Configure the connection with:

| Property                     | Description                                                                        |
|------------------------------|------------------------------------------------------------------------------------|
| `spring.datasource.url`      | JDBC URL of the PostgreSQL database (e.g. `jdbc:postgresql://localhost:5432/mydb`) |
| `spring.datasource.username` | Database username                                                                  |
| `spring.datasource.password` | Database password                                                                  |

### OAuth2

The application acts as an OAuth2 resource server and validates JWT tokens issued by an authorization server (e.g.
Keycloak). Configure it with:

| Property                                               | Description                                                      |
|--------------------------------------------------------|------------------------------------------------------------------|
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | URL of the JWT issuer (e.g. `http://localhost:8180/realms/lyra`) |

> **Local development:** run `./mvnw spring-boot:test-run` instead of `spring-boot:run`. It starts the application
> from the test classpath with `TestEnvironmentConfiguration` imported, which brings up PostgreSQL and Keycloak as
> Testcontainers-managed containers (see `src/test/java/edu/lyra/members/api/environment`), so no manual
> configuration or `docker compose` invocation is needed — only a running Docker daemon.

#### Scopes

Every mutating or read endpoint requires the caller's access token to carry the matching OAuth2 scope, exposed as a
`SCOPE_*` authority:

| Resource   | Create scope        | Read scope        | Update scope        | Delete scope        |
|------------|---------------------|-------------------|---------------------|---------------------|
| Parents    | `parents.create`    | `parents.read`    | `parents.update`    | `parents.delete`    |
| Kids       | `kids.create`       | `kids.read`       | `kids.update`       | `kids.delete`       |
| Schools    | `schools.create`    | `schools.read`    | `schools.update`    | `schools.delete`    |
| Teachers   | `teachers.create`   | `teachers.read`   | `teachers.update`   | `teachers.delete`   |
| Classrooms | `classrooms.create` | `classrooms.read` | `classrooms.update` | `classrooms.delete` |

`classrooms.update` also gates the classroom's teaching-staff and roster endpoints (adding/removing a teacher, setting
the tutor, enrolling a kid), and `parents.update` gates binding an existing kid to a parent
(`PUT /parents/{id}/kids/{kidId}`).

#### Roles

Beyond scopes, the token's `realm_access.roles` claim (mapped to `ROLE_*` authorities) determines *which*
records a caller may read or update, on top of holding the required scope:

| Role      | Entitlement                                                                                                                                                                                                                       |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `admin`   | Full access — can read and update any parent, kid, teacher, school, or classroom.                                                                                                                                                 |
| `parent`  | Can read and update only their own account, and only their own kids. Binding a kid to their own account is limited to a kid they themselves created.                                                                              |
| `teacher` | Can read the kids in classrooms they teach or tutor, but can only *update* a kid, or manage a classroom's roster/teaching staff, for classrooms where they are the **tutor**. Can read and update only their own teacher account. |

A caller with neither role (only a scope) can create records and read/update their own account where applicable, but
sees no kids and cannot update anyone else's records.

## Exploring the API

A [Postman](https://www.postman.com/) collection covering every endpoint lives at
`postman/members-api.postman_collection.json` — import it and point its `baseUrl` variable at a running instance to
explore the API. It is generated from the application's own OpenAPI description (`scripts/generate-postman-collection.sh`),
so the API is never defined twice; CI fails if it drifts from the code. After changing the API, regenerate it and
commit the result:

```shell
./scripts/generate-postman-collection.sh
```

The OpenAPI description itself is never served live (there is no `/v3/api-docs` endpoint) — it exists only as a
build-time artifact (see `OpenApiExportTest`) that feeds the collection above.

## Performance testing

Gatling simulations live under `src/test/java/edu/lyra/members/api/performance`. `SmokeSimulation` runs
automatically against every native image build on a pull request or `main` (`PerformanceSmokeIT`), gating the push
to the registry; it's skipped when the deliverable under test is the plain-JRE jar used for fast per-push feedback,
since JVM performance figures don't reflect the native image actually shipped. `LoadSimulation`, `StressSimulation`
and `SoakSimulation` back a nightly suite (`NightlyPerformanceIT`, `nightly-performance.yml`) that isn't scheduled
yet - the workflow's schedule trigger is commented out until it's needed; `workflow_dispatch` still lets you run it
by hand.

The easiest way to run a simulation yourself is through its `*IT` wrapper, the same way CI does: it starts (or
reuses) the Testcontainers environment and points Gatling at it automatically, so you never have to know the API's
current version segment or which port this run's Keycloak happens to be on.

```shell
./mvnw -Dit.test=PerformanceSmokeIT verify           # SmokeSimulation
./mvnw -Dit.test=NightlyPerformanceIT -Dperf.nightly=true verify   # Load/Stress/Soak
```

`perf.baseUrl` and `perf.tokenUrl` have no built-in default on purpose - hardcoding a version segment or a
Keycloak port here would silently go stale the moment either changes. To point a simulation at some other,
already-running instance instead, supply both yourself:

```shell
./mvnw gatling:test -Dgatling.simulationClass=edu.lyra.members.api.performance.SmokeSimulation \
    -Dperf.baseUrl=http://localhost:8080/v0 \
    -Dperf.tokenUrl=http://localhost:8180/realms/lyra/protocol/openid-connect/token
```
