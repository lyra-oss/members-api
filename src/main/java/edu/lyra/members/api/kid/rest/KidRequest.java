package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

record KidRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String surname,
        @Past @NotNull LocalDate birthdate
) {}
