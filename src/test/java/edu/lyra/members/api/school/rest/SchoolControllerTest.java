package edu.lyra.members.api.school.rest;

import java.util.UUID;

import edu.lyra.members.api.school.School;
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
class SchoolControllerTest {

    @Mock
    private SchoolAdapter adapter;

    @Mock
    private PagedResourcesAssembler<School> pagedAssembler;

    private SchoolController controller;

    @BeforeEach
    void setUp() {
        this.controller = new SchoolController(this.adapter, this.pagedAssembler);
    }

    @Test
    void findAllDelegatesToTheAdapter() {
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<SchoolModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void createReturnsCreatedWithTheSelfLinkAsLocation() {
        final SchoolModel model = new SchoolModel(UUID.randomUUID(), "Gloria Fuertes");
        model.add(Link.of("http://localhost/v0/schools/" + model.getId()).withSelfRel());
        final SchoolRequest request = new SchoolRequest("Gloria Fuertes");
        when(this.adapter.create(request)).thenReturn(model);
        final ResponseEntity<SchoolModel> response = this.controller.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/v0/schools/" + model.getId()));
        assertEquals(model, response.getBody());
    }

}
