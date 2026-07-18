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

> **Local development:** when running with `mvn spring-boot:run`, the `spring-boot-docker-compose` integration
> automatically starts the required PostgreSQL and Keycloak containers defined in `compose.yml`, so no manual
> configuration is needed.

#### Scopes

Every mutating or read endpoint requires the caller's access token to carry the matching OAuth2 scope, exposed
as a `SCOPE_*` authority:

| Resource   | Create scope      | Read scope        | Update scope        | Delete scope        |
|------------|-------------------|-------------------|---------------------|---------------------|
| Parents    | `parents.create`  | `parents.read`    | `parents.update`    | `parents.delete`    |
| Kids       | `kids.create`     | `kids.read`       | `kids.update`       | `kids.delete`       |
| Schools    | `schools.create`  | `schools.read`    | `schools.update`    | `schools.delete`    |
| Teachers   | `teachers.create` | `teachers.read`   | `teachers.update`   | `teachers.delete`   |
| Classrooms | —                 | `classrooms.read` | `classrooms.update` | `classrooms.delete` |

`classrooms.update` also gates the classroom's teaching-staff and roster endpoints (adding/removing a teacher,
setting the tutor, enrolling a kid), and `parents.update` gates binding an existing kid to a parent
(`POST /parents/{id}/kids`).

#### Roles

Beyond scopes, the token's `realm_access.roles` claim (mapped to `ROLE_*` authorities) determines *which*
records a caller may read or update, on top of holding the required scope:

| Role      | Entitlement                                                                                                                                                                                                                       |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `admin`   | Full access — can read and update any parent, kid, teacher, school, or classroom.                                                                                                                                                 |
| `parent`  | Can read and update only their own account, and only their own kids. Binding a kid to their own account is limited to a kid they themselves created.                                                                              |
| `teacher` | Can read the kids in classrooms they teach or tutor, but can only *update* a kid, or manage a classroom's roster/teaching staff, for classrooms where they are the **tutor**. Can read and update only their own teacher account. |

A caller with neither role (only a scope) can create records and read/update their own account where applicable,
but sees no kids and cannot update anyone else's records.
