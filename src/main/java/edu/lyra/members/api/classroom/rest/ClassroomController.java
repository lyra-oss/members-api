package edu.lyra.members.api.classroom.rest;

import java.net.URI;
import java.util.UUID;

import edu.lyra.members.api.classroom.Classroom;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/classrooms")
class ClassroomController {

    private final ClassroomAdapter                   adapter;
    private final PagedResourcesAssembler<Classroom> pagedAssembler;

    ClassroomController(final ClassroomAdapter adapter, final PagedResourcesAssembler<Classroom> pagedAssembler) {
        this.adapter        = adapter;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    PagedModel<ClassroomModel> findAll(final Pageable pageable) {
        log.debug("Listing classrooms, page {}", pageable);
        return this.adapter.findAll(pageable, this.pagedAssembler);
    }

    @GetMapping("/{id}")
    ResponseEntity<ClassroomModel> get(final @PathVariable UUID id) {
        log.debug("Fetching classroom {}", id);
        return this.adapter.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    ResponseEntity<ClassroomModel> create(final @Valid @RequestBody ClassroomRequest request) {
        log.debug("Creating a classroom");
        final ClassroomModel model    = this.adapter.create(request);
        final URI            location = model.getRequiredLink(IanaLinkRelations.SELF).toUri();
        return ResponseEntity.created(location).body(model);
    }

    @PatchMapping("/{id}")
    ResponseEntity<Void> update(final @PathVariable UUID id, final @Valid @RequestBody ClassroomPatchRequest request) {
        log.debug("Updating classroom {}", id);
        return this.adapter.update(id, request).isPresent() ? ResponseEntity.noContent().build() :
               ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(final @PathVariable UUID id) {
        log.debug("Deleting classroom {}", id);
        return this.adapter.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/teachers/{teacherId}")
    ResponseEntity<Void> addTeacher(final @PathVariable UUID id, final @PathVariable UUID teacherId) {
        log.debug("Adding teacher {} to classroom {}", teacherId, id);
        return this.adapter.addTeacher(id, teacherId) ? ResponseEntity.noContent().build() :
               ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/tutor/{teacherId}")
    ResponseEntity<Void> setTutor(final @PathVariable UUID id, final @PathVariable UUID teacherId) {
        log.debug("Setting teacher {} as tutor of classroom {}", teacherId, id);
        return this.adapter.setTutor(id, teacherId) ? ResponseEntity.noContent().build() :
               ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/kids/{kidId}")
    ResponseEntity<Void> enrollKid(final @PathVariable UUID id, final @PathVariable UUID kidId) {
        log.debug("Enrolling kid {} in classroom {}", kidId, id);
        return this.adapter.enrollKid(id, kidId) ? ResponseEntity.noContent().build() :
               ResponseEntity.notFound().build();
    }

}
