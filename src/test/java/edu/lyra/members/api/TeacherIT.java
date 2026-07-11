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

class TeacherIT
        extends BaseIT {

    private static final String SCHOOL_ADMIN_USERNAME = "school.admin@example.com";
    private static final String TEACHER_USERNAME       = "teacher.account@example.com";

    private static final String NAME_KEY    = "name";
    private static final String SURNAME_KEY = "surname";
    private static final String MAIL_KEY    = "mail";

    private static final String NAME_VALUE    = "Marta";
    private static final String SURNAME_VALUE = "Ibáñez";
    private static final String MAIL_VALUE    = TEACHER_USERNAME;

    @Test
    void testAddAndRetrieveTeacher()
            throws IOException {
        final String schoolLocation = this.createSchool();
        final String token = this.getToken(TEACHER_USERNAME, "teachers.create");
        final String body = this.json.writeValueAsString(
                Map.of(NAME_KEY, NAME_VALUE, SURNAME_KEY, SURNAME_VALUE, MAIL_KEY, MAIL_VALUE, "school",
                      schoolLocation));
        final Request postRequest = new Request.Builder().url("http://localhost:" + PORT + "/v0/teachers")
                                                         .addHeader("Authorization", "Bearer " + token)
                                                         .post(create(body, get("application/json"))).build();
        String location;
        try(Response response = this.http.newCall(postRequest).execute()) {
            assertEquals(201, response.code());
            location = response.header("Location");
            assertNotNull(location);
        }
        final Request getRequest =
                new Request.Builder().url(location).addHeader("Authorization", "Bearer " + token).build();
        try(Response response = this.http.newCall(getRequest).execute()) {
            assertEquals(200, response.code());
            final JsonNode node = this.json.readTree(response.body().string());
            assertEquals(NAME_VALUE, node.get(NAME_KEY).asString());
            assertEquals(SURNAME_VALUE, node.get(SURNAME_KEY).asString());
            assertEquals(MAIL_VALUE, node.get(MAIL_KEY).asString());
        }
    }

    private String createSchool()
            throws IOException {
        final String token = this.getToken(SCHOOL_ADMIN_USERNAME, "schools.create");
        final String body  = this.json.writeValueAsString(Map.of("name", "Gloria Fuertes"));
        final Request request = new Request.Builder().url("http://localhost:" + PORT + "/v0/schools")
                                                     .addHeader("Authorization", "Bearer " + token)
                                                     .post(create(body, get("application/json"))).build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(201, response.code());
            return response.header("Location");
        }
    }

}
