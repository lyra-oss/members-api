package edu.lyra.members.api.teacher.rest;

import java.util.Optional;
import java.util.UUID;

import edu.lyra.members.api.teacher.Teacher;
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
class TeacherAssociationsControllerTest {

    @Mock
    private TeacherAdapter adapter;

    @Mock
    private PagedResourcesAssembler<Teacher> pagedAssembler;

    private TeacherAssociationsController controller;

    @BeforeEach
    void setUp() {
        this.controller = new TeacherAssociationsController(this.adapter, this.pagedAssembler);
    }

    @Test
    void findBySchoolReturnsOkWhenTheSchoolExists() {
        final UUID schoolId = UUID.randomUUID();
        final Pageable pageable = Pageable.unpaged();
        final PagedModel<TeacherModel> expected = PagedModel.empty();
        when(this.adapter.findBySchool(schoolId, pageable, this.pagedAssembler)).thenReturn(Optional.of(expected));
        final ResponseEntity<PagedModel<TeacherModel>> response = this.controller.findBySchool(schoolId, pageable);
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

    @Test
    void findByClassroomReturnsNotFoundWhenTheClassroomIsMissing() {
        final UUID     classroomId = UUID.randomUUID();
        final Pageable pageable    = Pageable.unpaged();
        when(this.adapter.findByClassroom(classroomId, pageable, this.pagedAssembler)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.findByClassroom(classroomId, pageable).getStatusCode());
    }

}
