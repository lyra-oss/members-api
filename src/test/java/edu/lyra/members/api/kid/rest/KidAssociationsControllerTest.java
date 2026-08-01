package edu.lyra.members.api.kid.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.kid.Kid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KidAssociationsControllerTest {

    @Mock
    private KidAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Kid> pagedAssembler;

    private KidAssociationsController controller;

    @BeforeEach
    void setUp() {
        this.controller = new KidAssociationsController(this.adapter, this.pagedAssembler);
    }

    @Test
    void findByParentReturnsOkWhenTheParentExists() {
        final UUID parentId = UUID.randomUUID();
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<KidModel> expected = PagedModel.empty();
        when(this.adapter.findByParent(parentId, pageable, this.pagedAssembler)).thenReturn(Optional.of(expected));
        final ResponseEntity<PagedModel<KidModel>> response = this.controller.findByParent(parentId, pageable);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void findByParentReturnsNotFoundWhenTheParentIsMissing() {
        final UUID parentId = UUID.randomUUID();
        final Pageable pageable = Pageable.unpaged();
        when(this.adapter.findByParent(parentId, pageable, this.pagedAssembler)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.findByParent(parentId, pageable).getStatusCode());
    }

}
