package edu.lyra.members.api.parent.rest;

import java.net.URI;
import java.util.UUID;

import edu.lyra.members.api.parent.Parent;
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
@RequestMapping("/parents")
class ParentController {

    private final ParentAdapter                   adapter;
    private final PagedResourcesAssembler<Parent> pagedAssembler;

    ParentController(final ParentAdapter adapter, final PagedResourcesAssembler<Parent> pagedAssembler) {
        this.adapter        = adapter;
        this.pagedAssembler = pagedAssembler;
    }

    @GetMapping
    PagedModel<ParentModel> findAll(final Pageable pageable) {
        log.debug("Listing parents, page {}", pageable);
        return this.adapter.findAll(pageable, this.pagedAssembler);
    }

    @GetMapping("/{id}")
    ResponseEntity<ParentModel> get(final @PathVariable UUID id) {
        log.debug("Fetching parent {}", id);
        return this.adapter.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    ResponseEntity<ParentModel> create(final @Valid @RequestBody ParentRequest request) {
        log.debug("Registering a parent");
        final ParentModel model    = this.adapter.create(request);
        final URI         location = model.getRequiredLink(IanaLinkRelations.SELF).toUri();
        return ResponseEntity.created(location).body(model);
    }

    @PatchMapping("/{id}")
    ResponseEntity<Void> update(final @PathVariable UUID id, final @Valid @RequestBody ParentPatchRequest request) {
        log.debug("Updating parent {}", id);
        return this.adapter.update(id, request).isPresent() ? ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(final @PathVariable UUID id) {
        log.debug("Deleting parent {}", id);
        return this.adapter.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/kids/{kidId}")
    ResponseEntity<Void> bindKid(final @PathVariable UUID id, final @PathVariable UUID kidId) {
        log.debug("Binding kid {} to parent {}", kidId, id);
        return this.adapter.bindKid(id, kidId) ? ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }

}
