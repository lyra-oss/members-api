package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record KidPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String name,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String surname,
        @Past LocalDate birthdate,
        UUID parent,
        UUID classroom
) {}
