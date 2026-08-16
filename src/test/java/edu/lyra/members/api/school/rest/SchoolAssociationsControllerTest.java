package edu.lyra.members.api.school.rest;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAssociationsControllerTest {

    @Mock
    private SchoolAdapter adapter;

    private SchoolAssociationsController controller;

    @BeforeEach
    void setUp() {
        this.controller = new SchoolAssociationsController(this.adapter);
    }

    @Test
    void findByTeacherReturnsOkWhenTheTeacherExists() {
        final UUID        teacherId = UUID.randomUUID();
        final SchoolModel model     = aModel();
        when(this.adapter.findByTeacher(teacherId)).thenReturn(Optional.of(model));
        final ResponseEntity<SchoolModel> response = this.controller.findByTeacher(teacherId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
    }

    private static SchoolModel aModel() {
        return new SchoolModel(UUID.randomUUID(), "Gloria Fuertes");
    }

    @Test
    void findByTeacherReturnsNotFoundWhenTheTeacherIsMissing() {
        final UUID teacherId = UUID.randomUUID();
        when(this.adapter.findByTeacher(teacherId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.findByTeacher(teacherId).getStatusCode());
    }

    @Test
    void findByClassroomReturnsOkWhenTheClassroomExists() {
        final UUID        classroomId = UUID.randomUUID();
        final SchoolModel model       = aModel();
        when(this.adapter.findByClassroom(classroomId)).thenReturn(Optional.of(model));
        final ResponseEntity<SchoolModel> response = this.controller.findByClassroom(classroomId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(model, response.getBody());
    }

    @Test
    void findByClassroomReturnsNotFoundWhenTheClassroomIsMissing() {
        final UUID classroomId = UUID.randomUUID();
        when(this.adapter.findByClassroom(classroomId)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, this.controller.findByClassroom(classroomId).getStatusCode());
    }

}
