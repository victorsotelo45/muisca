package com.unicauca.muisca.application.dto;

import jakarta.validation.constraints.NotNull;

public record PublicationRequest(@NotNull Boolean published) {
}
