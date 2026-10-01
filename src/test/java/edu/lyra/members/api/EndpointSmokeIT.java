package edu.lyra.members.api;

import java.io.IOException;
import java.util.Map;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import static okhttp3.MediaType.get;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Touches every one of the application's HTTP-mapped routes at least once against the real, containerized
 * deliverable — verifying that routing, the security filter chain and persistence are wired correctly end to end.
 * Business logic and authorization edge cases are already exhaustively unit-tested (see e.g. {@code KidPolicyTest},
 * {@code EndpointCoverageTest}); this suite only checks that each route's happy path actually works when the
 * application is running for real against real PostgreSQL and Keycloak, which an in-process, mock-backed unit test
 * cannot.
 *
 * <p>One caller, {@code school.admin@example.com}, holds the realm's {@code admin} role, which every domain policy
 * in this codebase treats as an unconditional bypass — except {@code KidPolicy.authorizeCreate}, which requires the
 * caller to already be a registered parent, and self-registration ({@code POST /parents}, {@code POST /teachers},
 * {@code POST /kids}), which always binds to the caller's own identity. So the admin caller drives every other
 * route directly, while a handful of routes still need their own real identity.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class EndpointSmokeIT
        extends BaseIT {

    private static final String ADMIN_USERNAME   = "school.admin@example.com";
    private static final String PARENT_USERNAME  = "smoke.parent@example.com";
    private static final String TEACHER_USERNAME = "smoke.teacher@example.com";

    private static final String[] ADMIN_SCOPES = {
            "persons.read", "parents.create", "parents.read", "parents.update", "parents.delete", "teachers.create",
            "teachers.read", "teachers.update", "teachers.delete", "schools.create", "schools.read", "schools.update",
            "schools.delete", "classrooms.create", "classrooms.read", "classrooms.update", "classrooms.delete",
            "kids.create", "kids.read", "kids.update", "kids.delete"
    };

    private String adminToken;

    private String schoolId;
    private String throwawaySchoolId;
    private String parentId;
    private String teacherId;
    private String classroomId;
    private String throwawayClassroomId;
    private String kidId;
    private String throwawayTeacherId;
    private String throwawayParentId;
    private String throwawayKidId;

    @Test
    void everyEndpointRespondsSuccessfully()
            throws IOException {
        this.adminToken = this.getToken(ADMIN_USERNAME, ADMIN_SCOPES);
        this.createTopLevelResources();
        this.registerAdminThrowawayIdentities();
        this.managePersonRoles();
        this.bindClassroom();
        this.readEverything();
        this.updateEverything();
        this.deleteThrowaways();
    }

    private void createTopLevelResources()
            throws IOException {
        this.schoolId = this.postCreated("/schools", this.adminToken, Map.of("name", "Smoke Test School"));
        this.throwawaySchoolId =
                this.postCreated("/schools", this.adminToken, Map.of("name", "Smoke Test School (throwaway)"));

        final String parentToken = this.getToken(PARENT_USERNAME, "parents.create", "kids.create");
        this.parentId = this.postCreated("/parents", parentToken,
                Map.of("name", "Smoke", "surname", "Parent", "mail", PARENT_USERNAME));
        this.kidId = this.postCreated("/kids", parentToken,
                Map.of("name", "Smoke", "surname", "Kid", "birthdate", "2021-01-01"));

        final String teacherToken = this.getToken(TEACHER_USERNAME, "teachers.create");
        this.teacherId = this.postCreated("/teachers", teacherToken,
                Map.of("name", "Smoke", "surname", "Teacher", "mail", TEACHER_USERNAME, "school", this.schoolId));

        this.classroomId = this.postCreated("/classrooms", this.adminToken,
                Map.of("course", 1, "group", "A", "school", this.schoolId));
        this.throwawayClassroomId = this.postCreated("/classrooms", this.adminToken,
                Map.of("course", 2, "group", "B", "school", this.schoolId));
    }

    private void registerAdminThrowawayIdentities()
            throws IOException {
        this.throwawayTeacherId = this.postCreated("/teachers", this.adminToken,
                Map.of("name", "Throwaway", "surname", "Teacher", "mail", "throwaway.teacher@example.com", "school",
                        this.schoolId));
        this.throwawayParentId = this.postCreated("/parents", this.adminToken,
                Map.of("name", "Throwaway", "surname", "Parent", "mail", "throwaway.parent@example.com"));
        this.throwawayKidId = this.postCreated("/kids", this.adminToken,
                Map.of("name", "Throwaway", "surname", "Kid", "birthdate", "2021-01-01"));
    }

    private void managePersonRoles()
            throws IOException {
        this.putNoContent("/persons/" + this.parentId + "/teacher", this.adminToken, Map.of("school", this.schoolId));
        this.deleteNoContent("/persons/" + this.parentId + "/teacher", this.adminToken);
        this.putNoContent("/persons/" + this.teacherId + "/parent", this.adminToken);
        this.deleteNoContent("/persons/" + this.teacherId + "/parent", this.adminToken);
        this.getOk("/persons", this.adminToken);
        this.getOk("/persons/" + this.parentId, this.adminToken);
    }

    private void bindClassroom()
            throws IOException {
        this.putNoContent("/classrooms/" + this.classroomId + "/teachers/" + this.teacherId, this.adminToken);
        this.putNoContent("/classrooms/" + this.classroomId + "/tutor/" + this.teacherId, this.adminToken);
        this.putNoContent("/classrooms/" + this.classroomId + "/kids/" + this.kidId, this.adminToken);
        this.putNoContent("/parents/" + this.parentId + "/kids/" + this.kidId, this.adminToken);
    }

    private void readEverything()
            throws IOException {
        this.getOk("/", this.adminToken);

        this.getOk("/schools", this.adminToken);
        this.getOk("/schools/" + this.schoolId, this.adminToken);
        this.getOk("/classrooms", this.adminToken);
        this.getOk("/classrooms/" + this.classroomId, this.adminToken);
        this.getOk("/teachers", this.adminToken);
        this.getOk("/teachers/" + this.teacherId, this.adminToken);
        this.getOk("/parents", this.adminToken);
        this.getOk("/parents/" + this.parentId, this.adminToken);
        this.getOk("/kids", this.adminToken);
        this.getOk("/kids/" + this.kidId, this.adminToken);

        this.getOk("/kids/" + this.kidId + "/parent", this.adminToken);
        this.getOk("/teachers/" + this.teacherId + "/school", this.adminToken);
        this.getOk("/classrooms/" + this.classroomId + "/school", this.adminToken);
        this.getOk("/parents/" + this.parentId + "/kids", this.adminToken);
        this.getOk("/kids/" + this.kidId + "/classroom", this.adminToken);
        this.getOk("/schools/" + this.schoolId + "/classrooms", this.adminToken);
        this.getOk("/schools/" + this.schoolId + "/teachers", this.adminToken);
        this.getOk("/classrooms/" + this.classroomId + "/teachers", this.adminToken);
        this.getOk("/classrooms/" + this.classroomId + "/tutor", this.adminToken);
    }

    private void updateEverything()
            throws IOException {
        this.patchNoContent("/schools/" + this.schoolId, this.adminToken, Map.of("name", "Renamed Smoke Test School"));
        this.patchNoContent("/classrooms/" + this.classroomId, this.adminToken, Map.of("course", 3, "group", "C"));
        this.patchNoContent("/teachers/" + this.teacherId, this.adminToken, Map.of("name", "Renamed"));
        this.patchNoContent("/parents/" + this.parentId, this.adminToken, Map.of("name", "Renamed"));
        this.patchNoContent("/kids/" + this.kidId, this.adminToken, Map.of("name", "Renamed"));
    }

    private void deleteThrowaways()
            throws IOException {
        this.deleteNoContent("/kids/" + this.throwawayKidId, this.adminToken);
        this.deleteNoContent("/parents/" + this.throwawayParentId, this.adminToken);
        this.deleteNoContent("/teachers/" + this.throwawayTeacherId, this.adminToken);
        this.deleteNoContent("/classrooms/" + this.throwawayClassroomId, this.adminToken);
        this.deleteNoContent("/schools/" + this.throwawaySchoolId, this.adminToken);
    }

    private String postCreated(final String path, final String token, final Map<String, ?> body)
            throws IOException {
        //@formatter:off
        final Request request = new Request.Builder().url(BASE_URL + path)
                                                      .addHeader("Authorization", "Bearer " + token)
                                                      .post(create(this.json.writeValueAsString(body), get("application/json")))
                                                      .build();
        //@formatter:on
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(201, response.code(), path);
            return this.json.readTree(response.body().string()).get("id").asString();
        }
    }

    private void getOk(final String path, final String token)
            throws IOException {
        final Request request =
                new Request.Builder().url(BASE_URL + path).addHeader("Authorization", "Bearer " + token).build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(200, response.code(), path);
        }
    }

    private void putNoContent(final String path, final String token)
            throws IOException {
        //@formatter:off
        final Request request = new Request.Builder().url(BASE_URL + path)
                                                      .addHeader("Authorization", "Bearer " + token)
                                                      .put(create("", get("application/json")))
                                                      .build();
        //@formatter:on
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(204, response.code(), path);
        }
    }

    private void putNoContent(final String path, final String token, final Map<String, ?> body)
            throws IOException {
        //@formatter:off
        final Request request = new Request.Builder().url(BASE_URL + path)
                                                      .addHeader("Authorization", "Bearer " + token)
                                                      .put(create(this.json.writeValueAsString(body), get("application/json")))
                                                      .build();
        //@formatter:on
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(204, response.code(), path);
        }
    }

    private void patchNoContent(final String path, final String token, final Map<String, ?> body)
            throws IOException {
        //@formatter:off
        final Request request = new Request.Builder().url(BASE_URL + path)
                                                      .addHeader("Authorization", "Bearer " + token)
                                                      .patch(create(this.json.writeValueAsString(body), get("application/json")))
                                                      .build();
        //@formatter:on
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(204, response.code(), path);
        }
    }

    private void deleteNoContent(final String path, final String token)
            throws IOException {
        final Request request = new Request.Builder().url(BASE_URL + path)
                                                      .addHeader("Authorization", "Bearer " + token).delete().build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(204, response.code(), path);
        }
    }

}
