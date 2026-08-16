package edu.lyra.members.api.teacher.rest;

import java.util.UUID;

import edu.lyra.members.api.teacher.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
class TeacherAssociationsController {

    private final TeacherAdapter                   adapter;
    private final PagedResourcesAssembler<Teacher> pagedAssembler;

    @GetMapping("/schools/{schoolId}/teachers")
    ResponseEntity<PagedModel<TeacherModel>> findBySchool(final @PathVariable UUID schoolId, final Pageable pageable) {
        log.debug("Listing teachers for school {}, page {}", schoolId, pageable);
        return this.adapter.findBySchool(schoolId, pageable, this.pagedAssembler).map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/classrooms/{classroomId}/teachers")
    ResponseEntity<PagedModel<TeacherModel>> findByClassroom(
            final @PathVariable UUID classroomId,
            final Pageable pageable
    ) {
        log.debug("Listing teaching staff for classroom {}, page {}", classroomId, pageable);
        return this.adapter.findByClassroom(classroomId, pageable, this.pagedAssembler).map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/classrooms/{classroomId}/tutor")
    ResponseEntity<TeacherModel> findTutorOf(final @PathVariable UUID classroomId) {
        log.debug("Fetching the tutor of classroom {}", classroomId);
        return this.adapter.findTutorOf(classroomId).map(ResponseEntity::ok)
                           .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
