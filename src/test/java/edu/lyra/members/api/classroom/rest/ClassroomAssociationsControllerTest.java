package edu.lyra.members.api.classroom.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
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
class ClassroomAssociationsControllerTest {

    @Mock
    private ClassroomAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Classroom> pagedAssembler;

    private ClassroomAssociationsController controller;

    @BeforeEach
    void setUp() {
        this.controller = new ClassroomAssociationsController(this.adapter, this.pagedAssembler);
    }

    @Test
    void findByKidReturnsOkWhenTheKidsClassroomExists() {
        final UUID kidId = UUID.randomUUID();
        final ClassroomModel model = new ClassroomModel(UUID.randomUUID(), 3, "A");
        when(this.adapter.findByKid(kidId)).thenReturn(Optional.of(model));
        final ResponseEntity<ClassroomModel> response = this.controller.findByKid(kidId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
    }

    @Test
    void findByKidReturnsNotFoundWhenTheKidOrItsClassroomIsMissing() {
        final UUID kidId = UUID.randomUUID();
        when(this.adapter.findByKid(kidId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.findByKid(kidId).getStatusCode());
    }

    @Test
    void findBySchoolReturnsOkWhenTheSchoolExists() {
        final UUID schoolId = UUID.randomUUID();
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<ClassroomModel> expected = PagedModel.empty();
        when(this.adapter.findBySchool(schoolId, pageable, this.pagedAssembler)).thenReturn(Optional.of(expected));
        final ResponseEntity<PagedModel<ClassroomModel>> response = this.controller.findBySchool(schoolId, pageable);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
    }

    @Test
    void findBySchoolReturnsNotFoundWhenTheSchoolIsMissing() {
        final UUID schoolId = UUID.randomUUID();
        final Pageable pageable = Pageable.unpaged();
        when(this.adapter.findBySchool(schoolId, pageable, this.pagedAssembler)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.findBySchool(schoolId, pageable).getStatusCode());
    }

}
