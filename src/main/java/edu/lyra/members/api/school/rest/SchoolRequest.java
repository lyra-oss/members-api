package edu.lyra.members.api.school.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Shared by create and patch: School's create and update payloads happen to have the exact same shape
// (a single "name" field, always required), so a second near-identical record would add nothing.
record SchoolRequest(@NotBlank @Size(max = 100) String name) {}
