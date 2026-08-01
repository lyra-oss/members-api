package edu.lyra.members.api.school.rest;

import java.util.Optional;
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
import static org.mockito.Mockito.verify;
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
        final Pageable       pageable = Pageable.unpaged();
        final PagedModel<SchoolModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void getReturnsOkWhenTheSchoolExists() {
        final UUID id = UUID.randomUUID();
        final SchoolModel model = new SchoolModel(id, "Gloria Fuertes");
        when(this.adapter.findById(id)).thenReturn(Optional.of(model));
        final ResponseEntity<SchoolModel> response = this.controller.get(id);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
    }

    @Test
    void getReturnsNotFoundWhenTheSchoolIsMissing() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.findById(id)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.get(id).getStatusCode());
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

    @Test
    void updateReturnsNoContentWhenTheSchoolExists() {
        final UUID id = UUID.randomUUID();
        final SchoolRequest request = new SchoolRequest("New name");
        when(this.adapter.update(id, request)).thenReturn(Optional.of(new SchoolModel(id, "New name")));
        assertEquals(HttpStatus.NO_CONTENT, this.controller.update(id, request).getStatusCode());
    }

    @Test
    void updateReturnsNotFoundWhenTheSchoolIsMissing() {
        final UUID id = UUID.randomUUID();
        final SchoolRequest request = new SchoolRequest("New name");
        when(this.adapter.update(id, request)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.update(id, request).getStatusCode());
    }

    @Test
    void deleteReturnsNoContentWhenTheSchoolExisted() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.delete(id)).thenReturn(true);
        assertEquals(HttpStatus.NO_CONTENT, this.controller.delete(id).getStatusCode());
        verify(this.adapter).delete(id);
    }

    @Test
    void deleteReturnsNotFoundWhenTheSchoolDidNotExist() {
        final UUID id = UUID.randomUUID();
        when(this.adapter.delete(id)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.delete(id).getStatusCode());
    }

}
