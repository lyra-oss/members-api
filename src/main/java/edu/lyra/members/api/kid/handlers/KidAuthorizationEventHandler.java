package edu.lyra.members.api.kid.handlers;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.parent.ParentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.annotation.HandleBeforeCreate;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.security.access.AccessDeniedException;

@Slf4j
@RepositoryEventHandler
class KidAuthorizationEventHandler {

    private final ParentRepository parentRepository;

    KidAuthorizationEventHandler(final ParentRepository parentRepository) {
        this.parentRepository = parentRepository;
    }

    @HandleBeforeCreate
    public void assignAuthenticatedParent(final Kid kid) {
        log.debug("Assigning authenticated parent to kid before creation");
        kid.setParent(this.parentRepository.findById(AuthenticatedPrincipal.requireCurrentId()).orElseThrow(
                () -> new AccessDeniedException("Authenticated user cannot register this kid")));
    }

}
