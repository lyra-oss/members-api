package edu.lyra.members.api.kid.rest;

import java.util.List;

import edu.lyra.members.api.kid.Kid;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KidVisibilityStrategyResolverTest {

    @Mock
    private KidVisibilityStrategy unsupportingStrategy;

    @Mock
    private KidVisibilityStrategy supportingStrategy;

    @Test
    void delegatesToTheFirstStrategyThatSupportsTheRequest() {
        final Pageable  pageable = Pageable.unpaged();
        final Page<Kid> page     = new PageImpl<>(List.of(Instancio.create(Kid.class)));
        when(this.unsupportingStrategy.supports()).thenReturn(false);
        when(this.supportingStrategy.supports()).thenReturn(true);
        when(this.supportingStrategy.findVisible(pageable)).thenReturn(page);
        final KidVisibilityStrategyResolver resolver =
                new KidVisibilityStrategyResolver(List.of(this.unsupportingStrategy, this.supportingStrategy));
        assertEquals(page, resolver.resolve(pageable));
        verify(this.unsupportingStrategy, never()).findVisible(pageable);
    }

    @Test
    void returnsAnEmptyPageWhenNoStrategySupportsTheRequest() {
        final Pageable pageable = Pageable.unpaged();
        when(this.unsupportingStrategy.supports()).thenReturn(false);
        final KidVisibilityStrategyResolver resolver =
                new KidVisibilityStrategyResolver(List.of(this.unsupportingStrategy));
        final Page<Kid> result = resolver.resolve(pageable);
        assertTrue(result.isEmpty());
        verify(this.unsupportingStrategy, never()).findVisible(pageable);
    }

}
