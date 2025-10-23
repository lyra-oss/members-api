package com.sagittec.lyra.members.api.controllers;

import com.sagittec.lyra.members.api.repositories.School;
import com.sagittec.lyra.members.api.repositories.SchoolsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
class SchoolsController {

    private final SchoolsRepository schoolsRepository;

    @PostMapping("/schools")
    @ResponseStatus(CREATED)
    School createSchools(@RequestBody @Valid School school) {
        return this.schoolsRepository.save(school);
    }

}
