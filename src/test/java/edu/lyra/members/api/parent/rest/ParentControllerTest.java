package edu.lyra.members.api.parent.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentControllerTest {

    @Mock
    private ParentAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Parent> pagedAssembler;

    private ParentController controller;

    @BeforeEach
    void setUp() {
        this.controller = new ParentController(this.adapter, this.pagedAssembler);
    }

    private static ParentModel aModel(final UUID id) {
        return new ParentModel(id, "Esteban", "Cristóbal", "esteban.cristobal@example.com");
    }

    @Test
    void findAllDelegatesToTheAdapter() {
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<ParentModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void getReturnsOkWhenTheParentExists() {
        final UUID id = UUID.randomUUID();
        final ParentModel model = aModel(id);
        when(this.adapter.findById(id)).thenReturn(Optional.of(model));
        final ResponseEntity<ParentModel> response = this.controller.get(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
    }

    @Test
    void getReturnsNotFoundWhenTheParentIsMissing() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.findById(id)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.get(id).getStatusCode());
    }

    @Test
    void createReturnsCreatedWithTheSelfLinkAsLocation() {
        final ParentModel model = aModel(UUID.randomUUID());
        model.add(Link.of("http://localhost/v0/parents/" + model.getId()).withSelfRel());
        final ParentRequest request =
                new ParentRequest("Esteban", "Cristóbal", "esteban.cristobal@example.com");
        when(this.adapter.create(request)).thenReturn(model);
        final ResponseEntity<ParentModel> response = this.controller.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/v0/parents/" + model.getId()));
        assertEquals(model, response.getBody());
    }

    @Test
    void updateReturnsNoContentWhenTheParentExists() {
        final UUID id = UUID.randomUUID();
        final ParentPatchRequest request = new ParentPatchRequest(null, "New surname", null);
        when(this.adapter.update(id, request)).thenReturn(Optional.of(aModel(id)));
        assertEquals(HttpStatus.NO_CONTENT, this.controller.update(id, request).getStatusCode());
    }

    @Test
    void updateReturnsNotFoundWhenTheParentIsMissing() {
        final UUID id = UUID.randomUUID();
        final ParentPatchRequest request = new ParentPatchRequest(null, "New surname", null);
        when(this.adapter.update(id, request)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.update(id, request).getStatusCode());
    }

    @Test
    void deleteReturnsNoContentWhenTheParentExisted() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.delete(id)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, this.controller.delete(id).getStatusCode());
        verify(this.adapter).delete(id);
    }

    @Test
    void deleteReturnsNotFoundWhenTheParentDidNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.delete(id)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.delete(id).getStatusCode());
    }

    @Test
    void bindKidReturnsNoContentWhenBothExist() {
        final UUID id    = UUID.randomUUID();
        final UUID kidId = UUID.randomUUID();
        when(this.adapter.bindKid(id, kidId)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, this.controller.bindKid(id, kidId).getStatusCode());
    }

    @Test
    void bindKidReturnsNotFoundWhenEitherIsMissing() {
        final UUID id    = UUID.randomUUID();
        final UUID kidId = UUID.randomUUID();
        when(this.adapter.bindKid(id, kidId)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.bindKid(id, kidId).getStatusCode());
    }

}
