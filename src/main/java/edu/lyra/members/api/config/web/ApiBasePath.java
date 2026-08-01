package edu.lyra.members.api.config.web;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The API's versioned base path (e.g. {@code /v0}), read from {@code lyra.api.base-path}. Shared by the
 * security configuration's request matchers and by every vertical slice's HATEOAS link building, so the
 * two never drift apart.
 *
 * @param basePath the base path every versioned route is served under
 *
 * @author Esteban Cristóbal Rodríguez
 */
@Validated
@ConfigurationProperties("lyra.api")
public record ApiBasePath(@NotBlank String basePath) {}
