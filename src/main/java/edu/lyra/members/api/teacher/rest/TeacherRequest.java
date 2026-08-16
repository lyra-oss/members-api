package edu.lyra.members.api.teacher.rest;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record TeacherRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String surname,
        @Email @NotBlank @Size(max = 200) String mail,
        @NotNull UUID school
) {}
