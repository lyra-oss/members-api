package edu.lyra.members.api.kid.rest;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidsRepository;
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

@Slf4j
@RequiredArgsConstructor
@RepositoryRestController
class KidsCollectionController {

    private static final String ROLE_ADMIN   = "ROLE_admin";
    private static final String ROLE_PARENT  = "ROLE_parent";
    private static final String ROLE_TEACHER = "ROLE_teacher";

    private final KidsRepository kidsRepository;

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
        final JwtAuthenticationToken jwtAuth =
                (JwtAuthenticationToken) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication(),
                                                                "Spring Security should have rejected the request " +
                                                                "before reaching this controller");
        if(hasAuthority(jwtAuth, ROLE_ADMIN)) {
            log.debug("Principal holds {}; returning every kid", ROLE_ADMIN);
            return this.kidsRepository.findAll(pageable);
        }
        final Optional<UUID> principalId = principalId(jwtAuth);
        if(principalId.isEmpty()) {
            return Page.empty(pageable);
        }
        if(hasAuthority(jwtAuth, ROLE_PARENT)) {
            log.debug("Scoping kid list to parent {}", principalId.get());
            return this.kidsRepository.findByParentIdOrderByNameAsc(principalId.get(), pageable);
        }
        if(hasAuthority(jwtAuth, ROLE_TEACHER)) {
            log.debug("Scoping kid list to classrooms taught or tutored by teacher {}", principalId.get());
            return this.kidsRepository.findByClassroomTaughtOrTutoredBy(principalId.get(), pageable);
        }
        log.debug("Principal {} holds no recognised role; returning no kids", principalId.get());
        return Page.empty(pageable);
    }

    private static boolean hasAuthority(final Authentication authentication, final String authority) {
        return authentication.getAuthorities().stream()
                             .anyMatch(granted -> Objects.equals(granted.getAuthority(), authority));
    }

    private static Optional<UUID> principalId(final JwtAuthenticationToken jwtAuth) {
        try {
            return Optional.ofNullable(jwtAuth.getToken().getSubject()).map(UUID::fromString);
        } catch(final IllegalArgumentException _) {
            log.debug("JWT subject {} is not a valid UUID; returning no kids", jwtAuth.getToken().getSubject());
            return Optional.empty();
        }
    }

}
