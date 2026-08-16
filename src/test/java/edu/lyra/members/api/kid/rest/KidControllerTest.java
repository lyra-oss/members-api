package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.UUID;

import edu.lyra.members.api.kid.Kid;
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
class KidControllerTest {

    @Mock
    private KidAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Kid> pagedAssembler;

    private KidController controller;

    @BeforeEach
    void setUp() {
        this.controller = new KidController(this.adapter, this.pagedAssembler);
    }

    @Test
    void findAllDelegatesToTheAdapter() {
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<KidModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void createReturnsCreatedWithTheSelfLinkAsLocation() {
        final KidModel model = aModel(UUID.randomUUID());
        model.add(Link.of("http://localhost/v0/kids/" + model.getId()).withSelfRel());
        final KidRequest request = new KidRequest("Alicia", "Cristóbal", LocalDate.of(2019, 12, 12));
        when(this.adapter.create(request)).thenReturn(model);
        final ResponseEntity<KidModel> response = this.controller.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/v0/kids/" + model.getId()));
        assertEquals(model, response.getBody());
    }

    private static KidModel aModel(final UUID id) {
        return new KidModel(id, "Alicia", "Cristóbal", LocalDate.of(2019, 12, 12));
    }

}
