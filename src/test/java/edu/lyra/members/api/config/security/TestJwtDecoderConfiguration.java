package edu.lyra.members.api.config.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

// A JwtDecoder bean is required to boot the full application context (SpringSecurityConfiguration
// wires oauth2ResourceServer().jwt(...) unconditionally), but tests authenticate requests via the
// jwt() MockMvc post-processor instead of a real bearer token, so this decoder should never actually
// run - if it does, the resulting JwtException makes that failure obvious rather than masking it as
// an unrelated 401/403.
@TestConfiguration
public class TestJwtDecoderConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        return _ -> {
            throw new JwtException("Test JwtDecoder — use the jwt() post-processor instead of a real token");
        };
    }

}
