package edu.lyra.members.api.parent.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Every field is optional (PATCH only changes what is supplied) but, if supplied, must not be blank:
// most Jakarta constraints (including Pattern and Size) treat null as trivially valid and only
// constrain a non-null value, so omitting a field passes while sending "" or "   " does not.
record ParentPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String name,
        @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 100) String surname,
        @Email @Pattern(regexp = ".*\\S.*", message = "must not be blank") @Size(max = 200) String mail
) {}
