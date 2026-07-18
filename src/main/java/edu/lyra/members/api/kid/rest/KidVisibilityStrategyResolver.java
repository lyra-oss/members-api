package edu.lyra.members.api.kid.rest;

import java.util.List;

import edu.lyra.members.api.kid.Kid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class KidVisibilityStrategyResolver {

    private final List<KidVisibilityStrategy> strategies;

    KidVisibilityStrategyResolver(final List<KidVisibilityStrategy> strategies) {
        this.strategies = strategies;
    }

    Page<Kid> resolve(final Pageable pageable) {
        //@formatter:off
        return this.strategies.stream().filter(KidVisibilityStrategy::supports)
                              .findFirst()
                              .map(strategy -> strategy.findVisible(pageable))
                              .orElseGet(() -> Page.empty(pageable));
        //@formatter:on
    }

}
