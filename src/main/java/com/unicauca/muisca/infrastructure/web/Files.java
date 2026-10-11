package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.UploadedFile;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Conversión entre archivos HTTP (multipart / descargas) y los DTO de la aplicación. */
final class Files {

    static final MediaType XML = MediaType.APPLICATION_XML;
    static final MediaType ZIP = MediaType.parseMediaType("application/zip");
    static final MediaType TEXT = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

    private Files() {
    }

    static UploadedFile upload(MultipartFile file) {
        if (file == null) {
            return null;
        }
        try {
            return new UploadedFile(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo " + file.getOriginalFilename(), e);
        }
    }

    static ResponseEntity<byte[]> download(String filename, byte[] content, MediaType type) {
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(content);
    }
}
