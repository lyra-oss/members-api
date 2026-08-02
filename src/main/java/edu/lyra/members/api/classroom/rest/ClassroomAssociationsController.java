package edu.lyra.members.api.classroom.rest;

import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
class ClassroomAssociationsController {

    private final ClassroomAdapter                   adapter;
    private final PagedResourcesAssembler<Classroom> pagedAssembler;

    ClassroomAssociationsController(
            final ClassroomAdapter adapter,
            final PagedResourcesAssembler<Classroom> pagedAssembler
    ) {
        this.adapter        = adapter;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping("/kids/{kidId}/classroom")
    ResponseEntity<ClassroomModel> findByKid(final @PathVariable UUID kidId) {
        log.debug("Fetching the classroom of kid {}", kidId);
        return this.adapter.findByKid(kidId).map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/schools/{schoolId}/classrooms")
    ResponseEntity<PagedModel<ClassroomModel>> findBySchool(
            final @PathVariable UUID schoolId,
            final Pageable pageable
    ) {
        log.debug("Listing classrooms for school {}, page {}", schoolId, pageable);
        return this.adapter.findBySchool(schoolId, pageable, this.pagedAssembler).map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
