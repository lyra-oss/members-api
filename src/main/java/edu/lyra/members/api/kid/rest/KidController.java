package edu.lyra.members.api.kid.rest;

import java.net.URI;
import java.util.UUID;

import edu.lyra.members.api.config.web.ResponseEntities;
import edu.lyra.members.api.kid.Kid;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RestController
@RequestMapping("/kids")
class KidController {

    private final KidAdapter                   adapter;
    private final PagedResourcesAssembler<Kid> pagedAssembler;

    @GetMapping
    PagedModel<KidModel> findAll(final Pageable pageable) {
        log.debug("Listing kids visible to the authenticated principal, page {}", pageable);
        return this.adapter.findAll(pageable, this.pagedAssembler);
    }

    @GetMapping("/{id}")
    ResponseEntity<KidModel> get(final @PathVariable UUID id) {
        log.debug("Fetching kid {}", id);
        return ResponseEntities.okOrNotFound(this.adapter.findById(id));
    }

    @PostMapping
    ResponseEntity<KidModel> create(final @Valid @RequestBody KidRequest request) {
        log.debug("Registering a kid");
        final KidModel model    = this.adapter.create(request);
        final URI      location = model.getRequiredLink(IanaLinkRelations.SELF).toUri();
        return ResponseEntity.created(location).body(model);
    }

    @PatchMapping("/{id}")
    ResponseEntity<Void> update(final @PathVariable UUID id, final @Valid @RequestBody KidPatchRequest request) {
        log.debug("Updating kid {}", id);
        return ResponseEntities.noContentOrNotFound(this.adapter.update(id, request).isPresent());
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(final @PathVariable UUID id) {
        log.debug("Deleting kid {}", id);
        return ResponseEntities.noContentOrNotFound(this.adapter.delete(id));
    }

}
