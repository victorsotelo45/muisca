package com.unicauca.muisca.application.dto;

import jakarta.validation.constraints.NotBlank;

/** Token emitido por el proveedor OAuth (Google) que envía el cliente web. */
public record TokenDto(@NotBlank String value) {
}
