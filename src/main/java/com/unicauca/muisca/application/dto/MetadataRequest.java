package com.unicauca.muisca.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de nombre y descripción de un recurso (metamodelo, módulo o modelo guardado). */
public record MetadataRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 1000) String description) {
}
