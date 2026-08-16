package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.UUID;

import edu.lyra.members.api.config.web.NotBlankIfPresent;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

record KidPatchRequest(
        @NotBlankIfPresent @Size(max = 100) String name,
        @NotBlankIfPresent @Size(max = 100) String surname,
        @Past LocalDate birthdate,
        UUID parent,
        UUID classroom
) {}
