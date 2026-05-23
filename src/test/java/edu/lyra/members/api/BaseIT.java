package edu.lyra.members.api;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import tools.jackson.databind.ObjectMapper;

import static java.lang.Integer.parseInt;
import static java.lang.String.join;
import static java.lang.System.getProperty;

abstract class BaseIT {

    protected static final int PORT = parseInt(getProperty("it.port"));

    protected static final String KEYCLOAK_TOKEN_URL =
            "http://localhost:8180/realms/lyra/protocol/openid-connect/token";

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
        final Request request = new Request.Builder().url(KEYCLOAK_TOKEN_URL).post(body).build();
        try(Response response = this.http.newCall(request).execute()) {
            return this.json.readTree(response.body().string()).get("access_token").asString();
        }
    }

}
