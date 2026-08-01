package edu.lyra.members.api.kid.rest;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Every field is optional (PATCH only changes what is supplied): most Jakarta constraints (including
// Pattern, Size and Past) treat null as trivially valid and only constrain a non-null value, so
// omitting a field leaves the existing entity value untouched. parent/classroom are the new owner's
// bare id, resolved against the existing value by the adapter when omitted.
record KidPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String name,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String surname,
        @Past LocalDate birthdate,
        UUID parent,
        UUID classroom
) {}
