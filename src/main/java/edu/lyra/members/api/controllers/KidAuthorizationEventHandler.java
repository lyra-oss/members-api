package edu.lyra.members.api.controllers;

import java.util.Optional;

import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@RepositoryEventHandler
class KidAuthorizationEventHandler {

    private final ParentsRepository parentsRepository;

    KidAuthorizationEventHandler(final ParentsRepository parentsRepository) {
        this.parentsRepository = parentsRepository;
    }

    @HandleBeforeCreate
    public void validateParentOwnership(final Kid kid) {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AccessDeniedException("JWT authentication required");
        }
        Optional.ofNullable(jwtAuth.getToken().getSubject()).flatMap(this.parentsRepository::findByMail)
                .ifPresentOrElse(parent -> {
                    if(kid.getParent() != null && kid.getParent().getId() != parent.getId()) {
                        throw new AccessDeniedException("Cannot create a kid for a different parent");
                    }
                }, () -> {
                    throw new AccessDeniedException("Authenticated user cannot register this kid");
                });
    }

}
