package edu.lyra.members.api.config.web;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The API's versioned base path (e.g. {@code /v0}), read directly from {@code server.servlet.context-path}.
 * Security matchers and HATEOAS link building pick the context path up automatically from the container, so
 * this bean's sole remaining purpose is letting tests (where {@code MockMvc} does not derive the
 * context path on its own) ask what it is.
 *
 * @param contextPath the base path every versioned route is served under
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Validated
@ConfigurationProperties("server.servlet")
public record ApiBasePath(@NotBlank String contextPath) {}
