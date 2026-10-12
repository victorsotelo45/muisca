package com.unicauca.muisca.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** @param currentPassword obligatoria si la cuenta ya tiene contraseña local */
public record ChangePasswordRequest(
        String currentPassword,
        @NotBlank @Size(min = 6, max = 100) String newPassword) {
}
