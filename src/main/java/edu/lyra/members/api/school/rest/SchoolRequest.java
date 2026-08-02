package edu.lyra.members.api.school.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SchoolRequest(@NotBlank @Size(max = 100) String name) {}
