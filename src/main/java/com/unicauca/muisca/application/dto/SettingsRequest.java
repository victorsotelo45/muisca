package com.unicauca.muisca.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SettingsRequest(
        @NotNull Boolean registrationEnabled,
        @Size(max = 1000) String announcement) {
}
