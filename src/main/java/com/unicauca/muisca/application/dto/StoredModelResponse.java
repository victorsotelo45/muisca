package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.domain.model.StoredModel;

import java.time.LocalDateTime;

/** Modelo de la biblioteca, sin su contenido (se descarga en /api/models/{id}/content). */
public record StoredModelResponse(Long id, String name, String description, Reference metamodel, String fileName,
        long sizeBytes, String owner, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static StoredModelResponse from(StoredModel m) {
        return new StoredModelResponse(m.getId(), m.getName(), m.getDescription(),
                new Reference(m.getMetamodel().getId(), m.getMetamodel().getName()), m.getFileName(),
                m.getSizeBytes(), m.getOwner().getUsername(), m.getCreatedAt(), m.getUpdatedAt());
    }
}
