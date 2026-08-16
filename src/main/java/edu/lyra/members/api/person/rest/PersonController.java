package edu.lyra.members.api.person.rest;

import java.util.UUID;

import edu.lyra.members.api.config.web.ResponseEntities;
import edu.lyra.members.api.person.Person;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/persons")
class PersonController {

    private final PersonAdapter                   adapter;
    private final PagedResourcesAssembler<Person> pagedAssembler;

    @PreAuthorize("hasRole('admin')")
    @GetMapping
    PagedModel<PersonModel> findAll(final Pageable pageable) {
        log.debug("Listing persons, page {}", pageable);
        return this.adapter.findAll(pageable, this.pagedAssembler);
    }

    @PreAuthorize("hasRole('admin')")
    @GetMapping("/{id}")
    ResponseEntity<PersonModel> get(final @PathVariable UUID id) {
        log.debug("Fetching person {}", id);
        return ResponseEntities.okOrNotFound(this.adapter.findById(id));
    }

    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}/parent")
    ResponseEntity<Void> grantParentRole(final @PathVariable UUID id) {
        log.debug("Granting the parent role to person {}", id);
        return ResponseEntities.noContentOrNotFound(this.adapter.grantParentRole(id));
    }

    @PreAuthorize("hasRole('admin')")
    @PutMapping("/{id}/teacher")
    ResponseEntity<Void> grantTeacherRole(
            final @PathVariable UUID id,
            final @Valid @RequestBody GrantTeacherRoleRequest request
    ) {
        log.debug("Granting the teacher role to person {}", id);
        return ResponseEntities.noContentOrNotFound(this.adapter.grantTeacherRole(id, request));
    }

    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}/parent")
    ResponseEntity<Void> revokeParentRole(final @PathVariable UUID id) {
        log.debug("Revoking the parent role from person {}", id);
        return ResponseEntities.noContentOrNotFound(this.adapter.revokeParentRole(id));
    }

    @PreAuthorize("hasRole('admin')")
    @DeleteMapping("/{id}/teacher")
    ResponseEntity<Void> revokeTeacherRole(final @PathVariable UUID id) {
        log.debug("Revoking the teacher role from person {}", id);
        return ResponseEntities.noContentOrNotFound(this.adapter.revokeTeacherRole(id));
    }

}
