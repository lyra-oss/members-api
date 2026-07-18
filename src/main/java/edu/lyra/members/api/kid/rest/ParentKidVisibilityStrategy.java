package edu.lyra.members.api.kid.rest;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
class ParentKidVisibilityStrategy
        implements KidVisibilityStrategy {

    private final KidRepository kidRepository;

    @Override
    public boolean supports() {
        return AuthenticatedPrincipal.hasRole("parent") && AuthenticatedPrincipal.currentId().isPresent();
    }

    @Override
    public Page<Kid> findVisible(final Pageable pageable) {
        return this.kidRepository.findByParentIdOrderByNameAsc(AuthenticatedPrincipal.requireCurrentId(), pageable);
    }

}
