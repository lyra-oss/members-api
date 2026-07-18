package edu.lyra.members.api.kid.rest;

import edu.lyra.members.api.config.security.AuthenticatedPrincipal;
import edu.lyra.members.api.kid.Kid;
import edu.lyra.members.api.kid.KidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
class AdminKidVisibilityStrategy
        implements KidVisibilityStrategy {

    private final KidRepository kidRepository;

    @Override
    public boolean supports() {
        return AuthenticatedPrincipal.isAdmin();
    }

    @Override
    public Page<Kid> findVisible(final Pageable pageable) {
        return this.kidRepository.findAll(pageable);
    }

}
