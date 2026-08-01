package edu.lyra.members.api.classroom.rest;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// Every field is optional (PATCH only changes what is supplied): most Jakarta constraints (including
// Positive, Max and Pattern) treat null as trivially valid and only constrain a non-null value, so
// omitting a field leaves the existing entity value untouched.
record ClassroomPatchRequest(
        @Positive @Max(6) Integer course,
        @Pattern(regexp = "^[A-Z]$") String group
) {}
