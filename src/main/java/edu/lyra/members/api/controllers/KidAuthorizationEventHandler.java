package edu.lyra.members.api.controllers;

import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static java.util.Optional.ofNullable;

@RepositoryEventHandler
class KidAuthorizationEventHandler {

    private final ParentsRepository parentsRepository;

    KidAuthorizationEventHandler(final ParentsRepository parentsRepository) {
        this.parentsRepository = parentsRepository;
    }

    @HandleBeforeCreate
    public void assignAuthenticatedParent(final Kid kid) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException("JWT authentication required");
        }
        kid.setParent(ofNullable(jwtAuth.getToken().getSubject()).map(UUID::fromString)
                                                                 .flatMap(this.parentsRepository::findById).orElseThrow(
                        () -> new AccessDeniedException("Authenticated user cannot register this kid")));
    }

}
