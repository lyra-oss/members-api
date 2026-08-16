package edu.lyra.members.api.school.rest;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
class SchoolAssociationsController {

    private final SchoolAdapter adapter;

    SchoolAssociationsController(final SchoolAdapter adapter) {
        this.adapter = adapter;
    }

    @GetMapping("/teachers/{teacherId}/school")
    ResponseEntity<SchoolModel> findByTeacher(final @PathVariable UUID teacherId) {
        log.debug("Fetching the school of teacher {}", teacherId);
        return this.adapter.findByTeacher(teacherId).map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/classrooms/{classroomId}/school")
    ResponseEntity<SchoolModel> findByClassroom(final @PathVariable UUID classroomId) {
        log.debug("Fetching the school of classroom {}", classroomId);
        return this.adapter.findByClassroom(classroomId).map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
