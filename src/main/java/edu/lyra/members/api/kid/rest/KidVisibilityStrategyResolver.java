package edu.lyra.members.api.kid.rest;

import java.util.List;

import edu.lyra.members.api.kid.Kid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
class KidVisibilityStrategyResolver {

    private final List<KidVisibilityStrategy> strategies;

    Page<Kid> resolve(final Pageable pageable) {
        //@formatter:off
        return this.strategies.stream().filter(KidVisibilityStrategy::supports)
                              .findFirst()
                              .map(strategy -> strategy.findVisible(pageable))
                              .orElseGet(() -> Page.empty(pageable));
        //@formatter:on
    }

}
