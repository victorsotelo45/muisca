package com.unicauca.muisca.infraestructure.web;

import com.unicauca.muisca.domain.dto.AtlFileDTO;
import com.unicauca.muisca.domain.model.AtlFile;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.AtlFileRepository;
import com.unicauca.muisca.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/atl/files")
public class AtlFileController {

    @Autowired
    private AtlFileRepository atlFileRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam MultipartFile file,
            @RequestParam String fileType,
            Authentication authentication) {
        try {
            // Obtener el usuario autenticado
            String username = authentication.getName();
            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Convertir el archivo a un arreglo de bytes
            byte[] fileData = file.getBytes();

            // Guardar el archivo en la base de datos
            AtlFile atlFile = new AtlFile(file.getOriginalFilename(), fileType, fileData, user);
            atlFileRepository.save(atlFile);

            return ResponseEntity.ok("Archivo guardado exitosamente: " + file.getOriginalFilename());
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error al guardar el archivo: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<AtlFileDTO>> listFiles(Authentication authentication) {
        String username = authentication.getName();
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<AtlFileDTO> files = atlFileRepository.findByUser(user).stream()
                .map(file -> new AtlFileDTO(file.getId(), file.getFilename(), file.getFileType(), file.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(files);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) {
        AtlFile atlFile = atlFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + atlFile.getFilename())
                .body(atlFile.getFileData());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteFile(@PathVariable Long id) {
        AtlFile atlFile = atlFileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Archivo no encontrado"));

        atlFileRepository.delete(atlFile);
        return ResponseEntity.ok("Archivo eliminado exitosamente: " + atlFile.getFilename());
    }
}