package com.sagittec.lyra.members.api.controllers;

import com.sagittec.lyra.members.api.repositories.Parent;
import com.sagittec.lyra.members.api.repositories.ParentsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
class ParentsController {

    private final ParentsRepository parentsRepository;

    @PostMapping("/parents")
    @ResponseStatus(CREATED)
    Parent createParent(@RequestBody @Valid Parent parent) {
        return this.parentsRepository.save(parent);
    }

}
