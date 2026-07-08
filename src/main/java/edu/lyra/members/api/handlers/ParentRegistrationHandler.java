package edu.lyra.members.api.handlers;

import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Parent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Optional.ofNullable;

@Slf4j
@RepositoryEventHandler
class ParentRegistrationHandler {

    @HandleBeforeCreate
    public void setSubjectAsId(final Parent parent) {
        log.debug("Assigning authenticated subject as parent ID before creation");
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException("JWT authentication required");
        } else {
            //@formatter:off
            parent.setId(ofNullable(jwtAuth.getToken().getSubject()).map(UUID::fromString)
                    .orElseThrow(() -> new AccessDeniedException("Missing \"sub\" claim in JWT token")));
            //@formatter:on
        }
    }

}
