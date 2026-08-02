package edu.lyra.members.api.classroom.rest;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

record ClassroomRequest(
        @Positive @Max(6) int course,
        @NotNull @Pattern(regexp = "^[A-Z]$") String group,
        @NotNull UUID school,
        UUID tutor
) {}
