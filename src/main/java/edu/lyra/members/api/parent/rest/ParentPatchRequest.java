package edu.lyra.members.api.parent.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record ParentPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String name,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String surname,
        @Email @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 200) String mail
) {}
