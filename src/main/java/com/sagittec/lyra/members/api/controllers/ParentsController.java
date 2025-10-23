package com.sagittec.lyra.members.api.controllers;

import java.util.Set;

import com.sagittec.lyra.members.api.repositories.Kid;
import com.sagittec.lyra.members.api.repositories.Parent;
import com.sagittec.lyra.members.api.repositories.ParentsRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping
class ParentsController {

    private final ParentsRepository parentsRepository;

    @PostMapping("/parents/{id}/kids")
    @ResponseStatus(HttpStatus.CREATED)
    Parent addKids(final @PathVariable int id, final @RequestBody @NotEmpty Set<@Valid Kid> kids) {
        log.info("Received request to add Kids for parent with ID {}", id);
        return this.parentsRepository.findById(id).map(parent -> {
            kids.forEach(kid -> kid.setParent(parent));
            parent.setKids(kids);
            return parentsRepository.save(parent);
        }).orElseThrow();
    }

}
