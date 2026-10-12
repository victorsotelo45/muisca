package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.MetadataRequest;
import com.unicauca.muisca.application.dto.AcceleoModuleResponse;
import com.unicauca.muisca.application.service.AcceleoModuleService;
import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Plantillas modelo-a-texto en Acceleo que generan el código del contrato (MODEL_ENGINEER y ADMIN). */
@RestController
@RequestMapping("/api/acceleo-modules")
public class AcceleoModuleController {

    private final AcceleoModuleService acceleoModuleService;

    public AcceleoModuleController(AcceleoModuleService acceleoModuleService) {
        this.acceleoModuleService = acceleoModuleService;
    }

    @GetMapping
    public List<AcceleoModuleResponse> list() {
        return acceleoModuleService.list();
    }

    @GetMapping("/{id}")
    public AcceleoModuleResponse get(@PathVariable Long id) {
        return acceleoModuleService.get(id);
    }

    @GetMapping("/{id}/source")
    public ResponseEntity<byte[]> downloadSource(@PathVariable Long id) {
        AcceleoModule module = acceleoModuleService.find(id);
        return Files.download(module.getModuleName() + ".mtl",
                module.getTemplateSource().getBytes(StandardCharsets.UTF_8), Files.TEXT);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AcceleoModuleResponse create(@RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long metamodelId,
            @RequestParam MultipartFile templateFile,
            @CurrentUser UserEntity actor) {
        return acceleoModuleService.create(name, description, metamodelId, Files.upload(templateFile), actor);
    }

    @PutMapping(path = "/{id}/source", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AcceleoModuleResponse updateSource(@PathVariable Long id, @RequestParam MultipartFile templateFile,
            @CurrentUser UserEntity actor) {
        return acceleoModuleService.updateSource(id, Files.upload(templateFile), actor);
    }

    /** Cambia nombre y descripción (el archivo no se modifica). */
    @PatchMapping("/{id}")
    public AcceleoModuleResponse updateMetadata(@PathVariable Long id, @Valid @RequestBody MetadataRequest request,
            @CurrentUser UserEntity actor) {
        return acceleoModuleService.updateMetadata(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser UserEntity actor) {
        acceleoModuleService.delete(id, actor);
    }
}
