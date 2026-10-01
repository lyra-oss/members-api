package edu.lyra.members.api;

import java.io.IOException;

import edu.lyra.members.api.environment.IntegrationTestEnvironment;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tools.jackson.databind.ObjectMapper;

import static java.lang.String.join;

/**
 * Base class for the black-box {@code *IT} suite: every subclass talks HTTP to
 * {@link IntegrationTestEnvironment#APPLICATION}, the application under test running as its own container, the same
 * way any real client would.
 *
 * @author Esteban Cristóbal Rodríguez
 */
abstract class BaseIT {

    protected static final String BASE_URL = IntegrationTestEnvironment.APPLICATION.baseUrl();

    protected final OkHttpClient http = new OkHttpClient();
    protected final ObjectMapper json = new ObjectMapper();

    protected String getToken(final String username, final String... scopes)
            throws IOException {
        //@formatter:off
        final FormBody body = new FormBody.Builder().add("grant_type", "password")
                                                    .add("client_id", "members-api-test")
                                                    .add("username", username)
                                                    .add("password", "password")
                                                    .add("scope", join(" ", scopes))
                                                    .build();
        //@formatter:on
        final String tokenUrl = IntegrationTestEnvironment.KEYCLOAK.tokenEndpoint();
        final Request request = new Request.Builder().url(tokenUrl).post(body).build();
        try(Response response = this.http.newCall(request).execute()) {
            return this.json.readTree(response.body().string()).get("access_token").asString();
        }
    }

}
