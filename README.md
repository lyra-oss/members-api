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
