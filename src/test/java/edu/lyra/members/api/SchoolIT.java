package edu.lyra.members.api;

import java.io.IOException;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static okhttp3.MediaType.get;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SchoolIT
        extends BaseIT {

    private static final String USERNAME = "school.admin@example.com";

    private static final String NAME_KEY = "name";

    private static final String NAME_VALUE = "Gloria Fuertes";

    @Test
    void testAddAndRetrieveSchool()
            throws IOException {
        final String createToken = this.getToken(USERNAME, "schools.create");
        final String body  = this.json.writeValueAsString(Map.of(NAME_KEY, NAME_VALUE));
        final Request postRequest = new Request.Builder().url("http://localhost:" + PORT + "/v0/schools")
                                                         .addHeader("Authorization", "Bearer " + createToken)
                                                         .post(create(body, get("application/json"))).build();
        String location;
        try(Response response = this.http.newCall(postRequest).execute()) {
            assertEquals(201, response.code());
            location = response.header("Location");
            assertNotNull(location);
        }
        final String readToken = this.getToken(USERNAME, "schools.read");
        final Request getRequest =
                new Request.Builder().url(location).addHeader("Authorization", "Bearer " + readToken).build();
        try(Response response = this.http.newCall(getRequest).execute()) {
            assertEquals(200, response.code());
            final JsonNode node = this.json.readTree(response.body().string());
            assertEquals(NAME_VALUE, node.get(NAME_KEY).asString());
        }
    }

}
