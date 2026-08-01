package edu.lyra.members.api.parent.rest;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
class ParentAssociationsController {

    private final ParentAdapter adapter;

    ParentAssociationsController(final ParentAdapter adapter) {
        this.adapter = adapter;
    }

    @GetMapping("${lyra.api.base-path}/kids/{kidId}/parent")
    ResponseEntity<ParentModel> findByKid(final @PathVariable UUID kidId) {
        log.debug("Fetching the parent of kid {}", kidId);
        return this.adapter.findByKid(kidId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
