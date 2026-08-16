package edu.lyra.members.api.parent.rest;

import edu.lyra.members.api.config.web.NotBlankIfPresent;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

record ParentPatchRequest(
        @NotBlankIfPresent @Size(max = 100) String name,
        @NotBlankIfPresent @Size(max = 100) String surname,
        @Email @NotBlankIfPresent @Size(max = 200) String mail
) {}
