package edu.lyra.members.api.config.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@TestConfiguration
public class TestJwtDecoderConfiguration {

    @Bean
    JwtDecoder jwtDecoder() {
        return _ -> {
            throw new JwtException("Test JwtDecoder — use the jwt() post-processor instead of a real token");
        };
    }

}
