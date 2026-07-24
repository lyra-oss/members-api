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
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KidsCollectionControllerTest {

    @Mock
    private KidVisibilityStrategyResolver visibilityResolver;

    @Mock
    private PersistentEntityResourceAssembler assembler;

    @Mock
    private PagedResourcesAssembler<Kid> pagedAssembler;

    @Test
    void findAllReturnsThePagedModelBuiltFromTheVisibleKids() {
        final Pageable pageable = Pageable.unpaged();
        final Page<Kid> page = new PageImpl<>(List.of(Instancio.create(Kid.class)));
        @SuppressWarnings("unchecked")
        final PagedModel<PersistentEntityResource> expected = mock(PagedModel.class);
        when(this.visibilityResolver.resolve(pageable)).thenReturn(page);
        when(this.pagedAssembler.<PersistentEntityResource>toModel(eq(page), any(RepresentationModelAssembler.class)))
                .thenReturn(expected);

        final KidsCollectionController controller = new KidsCollectionController(this.visibilityResolver);
        final PagedModel<PersistentEntityResource> result =
                controller.findAll(pageable, this.assembler, this.pagedAssembler);

        assertSame(expected, result);
    }

}
