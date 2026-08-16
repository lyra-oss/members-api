package edu.lyra.members.api.parent.rest;

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

    @Test
    void findAllDelegatesToTheAdapter() {
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<ParentModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void createReturnsCreatedWithTheSelfLinkAsLocation() {
        final ParentModel model = aModel(UUID.randomUUID());
        model.add(Link.of("http://localhost/v0/parents/" + model.getId()).withSelfRel());
        final ParentRequest request = new ParentRequest("Esteban", "Cristóbal", "esteban.cristobal@example.com");
        when(this.adapter.create(request)).thenReturn(model);
        final ResponseEntity<ParentModel> response = this.controller.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/v0/parents/" + model.getId()));
        assertEquals(model, response.getBody());
    }

    private static ParentModel aModel(final UUID id) {
        return new ParentModel(id, "Esteban", "Cristóbal", "esteban.cristobal@example.com");
    }

    @Test
    void bindKidReturnsNotFoundWhenEitherIsMissing() {
        final UUID id    = UUID.randomUUID();
        final UUID kidId = UUID.randomUUID();
        when(this.adapter.bindKid(id, kidId)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.bindKid(id, kidId).getStatusCode());
    }

}
