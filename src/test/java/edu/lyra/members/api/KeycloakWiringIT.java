package edu.lyra.members.api;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The bare minimum needed to verify the deployed application's Keycloak wiring is correct — that scopes and realm
 * roles from a real token actually reach {@code RequiredAccessAuthorizationManager} as {@code SCOPE_}/{@code ROLE_}
 * authorities. Everything else about authorization (which scope or role each endpoint requires) is a unit-testable
 * decision already covered by {@code RequiredAccessAuthorizationManagerTest} and the architecture suite; only the
 * end-to-end wiring itself needs a real identity provider.
 *
 * @author Esteban Cristóbal Rodríguez
 */
class KeycloakWiringIT
        extends BaseIT {

    @Test
    void aTokenMissingTheRequiredScopeIsForbidden()
            throws IOException {
        final String token = this.getToken("kid.parent@example.com", "kids.read");
        final Request request = new Request.Builder().url(BASE_URL + "/parents")
                                                     .addHeader("Authorization", "Bearer " + token).build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(403, response.code());
        }
    }

    @Test
    void aTokenWithTheScopeButNotTheRequiredRealmRoleIsForbidden()
            throws IOException {
        final String token = this.getToken("kid.parent@example.com", "persons.read");
        final Request request = new Request.Builder().url(BASE_URL + "/persons")
                                                     .addHeader("Authorization", "Bearer " + token).build();
        try(Response response = this.http.newCall(request).execute()) {
            assertEquals(403, response.code());
        }
    }

}
