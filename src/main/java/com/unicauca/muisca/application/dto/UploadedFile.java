package com.unicauca.muisca.application.dto;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/** Archivo recibido del cliente, desacoplado de la API de Servlets. */
public record UploadedFile(String filename, byte[] content) {

    public UploadedFile {
        filename = filename == null || filename.isBlank() ? "archivo" : filename;
        content = content == null ? new byte[0] : content;
    }

    /** @throws IllegalArgumentException si el archivo está vacío o no tiene una de las extensiones dadas */
    public UploadedFile requireExtension(String... extensions) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (Arrays.stream(extensions).noneMatch(lower::endsWith)) {
            throw new IllegalArgumentException("El archivo '" + filename + "' debe tener extensión "
                    + String.join(" o ", extensions));
        }
        if (content.length == 0) {
            throw new IllegalArgumentException("El archivo '" + filename + "' está vacío");
        }
        return this;
    }

    public String text() {
        return new String(content, StandardCharsets.UTF_8);
    }
}
