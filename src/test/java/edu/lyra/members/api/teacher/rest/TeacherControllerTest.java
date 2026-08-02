package edu.lyra.members.api.teacher.rest;

import java.util.UUID;

import edu.lyra.members.api.teacher.Teacher;
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
class TeacherControllerTest {

    @Mock
    private TeacherAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Teacher> pagedAssembler;

    private TeacherController controller;

    @BeforeEach
    void setUp() {
        this.controller = new TeacherController(this.adapter, this.pagedAssembler);
    }

    private static TeacherModel aModel(final UUID id) {
        return new TeacherModel(id, "Marta", "Ibáñez", "marta.ibanez@example.com");
    }

    @Test
    void findAllDelegatesToTheAdapter() {
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<TeacherModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void createReturnsCreatedWithTheSelfLinkAsLocation() {
        final TeacherModel model = aModel(UUID.randomUUID());
        model.add(Link.of("http://localhost/v0/teachers/" + model.getId()).withSelfRel());
        final TeacherRequest request =
                new TeacherRequest("Marta", "Ibáñez", "marta.ibanez@example.com", UUID.randomUUID());
        when(this.adapter.create(request)).thenReturn(model);
        final ResponseEntity<TeacherModel> response = this.controller.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/v0/teachers/" + model.getId()));
        assertEquals(model, response.getBody());
    }

}
