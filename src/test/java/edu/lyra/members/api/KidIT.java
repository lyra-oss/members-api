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

class KidIT
        extends BaseIT {

    private static final String USERNAME = "kid.parent@example.com";

    private static final String NAME_KEY      = "name";
    private static final String SURNAME_KEY   = "surname";
    private static final String BIRTHDATE_KEY = "birthdate";

    private static final String NAME_VALUE      = "Alicia";
    private static final String SURNAME_VALUE   = "Parent";
    private static final String BIRTHDATE_VALUE = "2020-06-15";

    @Test
    void testAddAndRetrieveKid()
            throws IOException {
        this.createParent();
        final String kidLocation = this.createKid();
        retrieveAndVerifyKid(kidLocation);
    }

    private void createParent()
            throws IOException {
        final String token = this.getToken(USERNAME, "parents.create");
        final String body  = this.json.writeValueAsString(Map.of("name", "Kid", "surname", "Parent", "mail", USERNAME));
        final Request request = new Request.Builder().url("http://localhost:" + PORT + "/v0/parents")
                                                     .addHeader("Authorization", "Bearer " + token)
                                                     .post(create(body, get("application/json"))).build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(201, response.code());
        }
    }

    private String createKid()
            throws IOException {
        final String token = this.getToken(USERNAME, "kids.create");
        final String body = this.json.writeValueAsString(
                Map.of(NAME_KEY, NAME_VALUE, SURNAME_KEY, SURNAME_VALUE, BIRTHDATE_KEY, BIRTHDATE_VALUE));
        final Request request = new Request.Builder().url("http://localhost:" + PORT + "/v0/kids")
                                                     .addHeader("Authorization", "Bearer " + token)
                                                     .post(create(body, get("application/json"))).build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(201, response.code());
            final String location = response.header("Location");
            assertNotNull(location);
            return location;
        }
    }

    private void retrieveAndVerifyKid(final String kidUrl)
            throws IOException {
        final String token = this.getToken(USERNAME, "kids.create");
        final Request request = new Request.Builder().url(kidUrl).addHeader("Authorization", "Bearer " + token).build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(200, response.code());
            final JsonNode node = this.json.readTree(response.body().string());
            assertEquals(NAME_VALUE, node.get(NAME_KEY).asString());
            assertEquals(SURNAME_VALUE, node.get(SURNAME_KEY).asString());
            assertEquals(BIRTHDATE_VALUE, node.get(BIRTHDATE_KEY).asString());
        }
    }

}
