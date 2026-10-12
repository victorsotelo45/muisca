package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.domain.model.Role;
import jakarta.validation.constraints.Size;

/** Campos opcionales: solo se actualizan los que vienen informados. */
public record UpdateUserRequest(
        @Size(min = 1, max = 100) String username,
        @Size(min = 6, max = 100) String password,
        Role role) {
}
