package edu.lyra.members.api.mvc;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.repositories.jpa.Kid;
import edu.lyra.members.api.repositories.jpa.KidsRepository;
import edu.lyra.members.api.repositories.jpa.ParentsRepository;
import edu.lyra.members.api.repositories.jpa.TeachersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static java.util.UUID.fromString;

@Slf4j
@RequiredArgsConstructor
@RepositoryRestController
class KidsCollectionController {

    private final KidsRepository     kidsRepository;
    private final ParentsRepository  parentsRepository;
    private final TeachersRepository teachersRepository;

    @ResponseBody
    @GetMapping("/kids")
    PagedModel<PersistentEntityResource> findAll(
            final Pageable pageable,
            final PersistentEntityResourceAssembler assembler,
            final PagedResourcesAssembler<Kid> pagedAssembler
    ) {
        final Page<Kid> page = this.kidsVisibleToAuthenticatedPrincipal(pageable);
        log.debug("Returning {} of {} kids visible to the authenticated principal", page.getNumberOfElements(),
                  page.getTotalElements());
        return pagedAssembler.toModel(page, assembler::toModel);
    }

    private Page<Kid> kidsVisibleToAuthenticatedPrincipal(final Pageable pageable) {
        return authenticatedPrincipalId().flatMap(principalId -> this.kidsFor(principalId, pageable))
                                         .orElseGet(() -> Page.empty(pageable));
    }

    private static Optional<UUID> authenticatedPrincipalId() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(! (authentication instanceof JwtAuthenticationToken jwtAuth)) {
            log.debug("No JWT authentication present; returning no kids");
            return Optional.empty();
        }
        try {
            return Optional.of(fromString(jwtAuth.getToken().getSubject()));
        } catch(final IllegalArgumentException e) {
            log.debug("JWT subject {} is not a valid UUID; returning no kids", jwtAuth.getToken().getSubject());
            return Optional.empty();
        }
    }

    private Optional<Page<Kid>> kidsFor(final UUID principalId, final Pageable pageable) {
        if(this.parentsRepository.existsById(principalId)) {
            log.debug("Scoping kid list to parent {}", principalId);
            return Optional.of(this.kidsRepository.findByParentIdOrderByNameAsc(principalId, pageable));
        }
        if(this.teachersRepository.existsById(principalId)) {
            log.debug("Scoping kid list to classrooms taught or tutored by teacher {}", principalId);
            return Optional.of(this.kidsRepository.findByClassroomTaughtOrTutoredBy(principalId, pageable));
        }
        log.debug("Principal {} is neither a parent nor a teacher; returning no kids", principalId);
        return Optional.empty();
    }

}
