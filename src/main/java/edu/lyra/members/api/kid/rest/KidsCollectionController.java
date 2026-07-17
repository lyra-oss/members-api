package edu.lyra.members.api.kid.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@RequiredArgsConstructor
@RepositoryRestController
class KidsCollectionController {

    private final KidRepository kidRepository;

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
        if(AuthenticatedPrincipal.hasRole("admin")) {
            log.debug("Principal holds the admin role; returning every kid");
            return this.kidRepository.findAll(pageable);
        }
        final Optional<UUID> principalId = AuthenticatedPrincipal.currentId();
        if(principalId.isEmpty()) {
            return Page.empty(pageable);
        }
        if(AuthenticatedPrincipal.hasRole("parent")) {
            log.debug("Scoping kid list to parent {}", principalId.get());
            return this.kidRepository.findByParentIdOrderByNameAsc(principalId.get(), pageable);
        }
        if(AuthenticatedPrincipal.hasRole("teacher")) {
            log.debug("Scoping kid list to classrooms taught or tutored by teacher {}", principalId.get());
            return this.kidRepository.findByClassroomTaughtOrTutoredBy(principalId.get(), pageable);
        }
        log.debug("Principal {} holds no recognised role; returning no kids", principalId.get());
        return Page.empty(pageable);
    }

}
