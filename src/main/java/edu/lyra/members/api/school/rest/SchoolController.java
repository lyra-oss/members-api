package edu.lyra.members.api.school.rest;

import java.net.URI;
import java.util.UUID;

import edu.lyra.members.api.school.School;
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
@RequestMapping("${lyra.api.base-path}/schools")
class SchoolController {

    private final SchoolAdapter                   adapter;
    private final PagedResourcesAssembler<School> pagedAssembler;

    SchoolController(final SchoolAdapter adapter, final PagedResourcesAssembler<School> pagedAssembler) {
        this.adapter        = adapter;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    PagedModel<SchoolModel> findAll(final Pageable pageable) {
        log.debug("Listing schools, page {}", pageable);
        return this.adapter.findAll(pageable, this.pagedAssembler);
    }

    @GetMapping("/{id}")
    ResponseEntity<SchoolModel> get(final @PathVariable UUID id) {
        log.debug("Fetching school {}", id);
        return this.adapter.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    ResponseEntity<SchoolModel> create(final @Valid @RequestBody SchoolRequest request) {
        log.debug("Creating a school");
        final SchoolModel model    = this.adapter.create(request);
        final URI         location = model.getRequiredLink(IanaLinkRelations.SELF).toUri();
        return ResponseEntity.created(location).body(model);
    }

    @PatchMapping("/{id}")
    ResponseEntity<Void> update(final @PathVariable UUID id, final @Valid @RequestBody SchoolRequest request) {
        log.debug("Updating school {}", id);
        return this.adapter.update(id, request).isPresent() ? ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(final @PathVariable UUID id) {
        log.debug("Deleting school {}", id);
        return this.adapter.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

}
