package edu.lyra.members.api.classroom.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

record ClassroomPatchRequest(
        @Positive @Max(6) Integer course,
        @Pattern(regexp = "^[A-Z]$") String group
) {}
