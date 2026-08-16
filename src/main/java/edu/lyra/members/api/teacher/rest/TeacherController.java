package edu.lyra.members.api.teacher.rest;

import java.net.URI;
import java.util.UUID;

import edu.lyra.members.api.teacher.Teacher;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/teachers")
class TeacherController {

    private final TeacherAdapter                   adapter;
    private final PagedResourcesAssembler<Teacher> pagedAssembler;

    TeacherController(final TeacherAdapter adapter, final PagedResourcesAssembler<Teacher> pagedAssembler) {
        this.adapter        = adapter;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    PagedModel<TeacherModel> findAll(final Pageable pageable) {
        log.debug("Listing teachers, page {}", pageable);
        return this.adapter.findAll(pageable, this.pagedAssembler);
    }

    @GetMapping("/{id}")
    ResponseEntity<TeacherModel> get(final @PathVariable UUID id) {
        log.debug("Fetching teacher {}", id);
        return this.adapter.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    ResponseEntity<TeacherModel> create(final @Valid @RequestBody TeacherRequest request) {
        log.debug("Registering a teacher");
        final TeacherModel model    = this.adapter.create(request);
        final URI          location = model.getRequiredLink(IanaLinkRelations.SELF).toUri();
        return ResponseEntity.created(location).body(model);
    }

    @PatchMapping("/{id}")
    ResponseEntity<Void> update(final @PathVariable UUID id, final @Valid @RequestBody TeacherPatchRequest request) {
        log.debug("Updating teacher {}", id);
        return this.adapter.update(id, request).isPresent() ? ResponseEntity.noContent().build() :
               ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(final @PathVariable UUID id) {
        log.debug("Deleting teacher {}", id);
        return this.adapter.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

}
