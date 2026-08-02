package edu.lyra.members.api.classroom.rest;

import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
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
class ClassroomControllerTest {

    @Mock
    private ClassroomAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Classroom> pagedAssembler;

    private ClassroomController controller;

    @BeforeEach
    void setUp() {
        this.controller = new ClassroomController(this.adapter, this.pagedAssembler);
    }

    private static ClassroomModel aModel(final UUID id) {
        return new ClassroomModel(id, 3, "A");
    }

    @Test
    void findAllDelegatesToTheAdapter() {
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<ClassroomModel> expected = PagedModel.empty();
        when(this.adapter.findAll(pageable, this.pagedAssembler)).thenReturn(expected);
        assertEquals(expected, this.controller.findAll(pageable));
    }

    @Test
    void createReturnsCreatedWithTheSelfLinkAsLocation() {
        final ClassroomModel model = aModel(UUID.randomUUID());
        model.add(Link.of("http://localhost/v0/classrooms/" + model.getId()).withSelfRel());
        final ClassroomRequest request = new ClassroomRequest(3, "A", UUID.randomUUID(), null);
        when(this.adapter.create(request)).thenReturn(model);
        final ResponseEntity<ClassroomModel> response = this.controller.create(request);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/v0/classrooms/" + model.getId()));
        assertEquals(model, response.getBody());
    }

    @Test
    void setTutorReturnsNotFoundWhenEitherIsMissing() {
        final UUID id        = UUID.randomUUID();
        final UUID teacherId = UUID.randomUUID();
        when(this.adapter.setTutor(id, teacherId)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.setTutor(id, teacherId).getStatusCode());
    }

    @Test
    void enrollKidReturnsNotFoundWhenEitherIsMissing() {
        final UUID id    = UUID.randomUUID();
        final UUID kidId = UUID.randomUUID();
        when(this.adapter.enrollKid(id, kidId)).thenReturn(false);
        assertEquals(HttpStatus.NOT_FOUND, this.controller.enrollKid(id, kidId).getStatusCode());
    }

}
