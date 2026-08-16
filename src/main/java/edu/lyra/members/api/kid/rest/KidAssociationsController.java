package edu.lyra.members.api.kid.rest;

import java.util.UUID;

import edu.lyra.members.api.config.web.ResponseEntities;
import edu.lyra.members.api.kid.Kid;
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
class KidAssociationsController {

    private final KidAdapter                   adapter;
    private final PagedResourcesAssembler<Kid> pagedAssembler;

    @GetMapping("/parents/{parentId}/kids")
    ResponseEntity<PagedModel<KidModel>> findByParent(final @PathVariable UUID parentId, final Pageable pageable) {
        log.debug("Listing kids for parent {}, page {}", parentId, pageable);
        return ResponseEntities.okOrNotFound(this.adapter.findByParent(parentId, pageable, this.pagedAssembler));
    }

}
