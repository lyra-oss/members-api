package edu.lyra.members.api.parent.rest;

import java.util.UUID;

import edu.lyra.members.api.config.web.ResponseEntities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
class ParentAssociationsController {

    private final ParentAdapter adapter;

    @GetMapping("/kids/{kidId}/parent")
    ResponseEntity<ParentModel> findByKid(final @PathVariable UUID kidId) {
        log.debug("Fetching the parent of kid {}", kidId);
        return ResponseEntities.okOrNotFound(this.adapter.findByKid(kidId));
    }

}
