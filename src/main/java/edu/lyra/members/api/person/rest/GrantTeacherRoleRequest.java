package edu.lyra.members.api.person.rest;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

record GrantTeacherRoleRequest(@NotNull UUID school) {}
