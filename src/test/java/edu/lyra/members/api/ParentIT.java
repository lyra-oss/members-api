package edu.lyra.members.api;

import java.io.IOException;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static okhttp3.MediaType.get;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = { "spring.jpa.hibernate.ddl-auto=create-drop", "management.otlp.metrics.export.enabled=false" }
)
class ParentIT {

    private static final String MAIL_VALUE    = "jane.doe@example.com";
    private static final String MAIL_KEY      = "mail";
    private static final String SURNAME_VALUE = "Doe";
    private static final String SURNAME_KEY   = "surname";
    private static final String NAME_VALUE    = "Jane";
    private static final String NAME_KEY      = "name";

    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper json = new ObjectMapper();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:latest");

    @LocalServerPort
    int port;

    @Test
    void testAddAndRetrieveParent()
            throws IOException {
        final String body = this.json.writeValueAsString(
                Map.of(NAME_KEY, NAME_VALUE, SURNAME_KEY, SURNAME_VALUE, MAIL_KEY, MAIL_VALUE));
        final Request postRequest = new Request.Builder().url("http://localhost:" + port + "/v0/parents")
                                                         .post(create(body, get("application/json"))).build();
        String location;
        try(Response response = this.http.newCall(postRequest).execute()) {
            assertEquals(201, response.code());
            location = response.header("Location");
            assertNotNull(location);
        }
        final Request getRequest = new Request.Builder().url(location).build();
        try(Response response = this.http.newCall(getRequest).execute()) {
            assertEquals(200, response.code());
            final JsonNode node = this.json.readTree(response.body().string());
            assertEquals(NAME_VALUE, node.get(NAME_KEY).asString());
            assertEquals(SURNAME_VALUE, node.get(SURNAME_KEY).asString());
            assertEquals(MAIL_VALUE, node.get(MAIL_KEY).asString());
        }
    }

}
