package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.MetadataRequest;
import com.unicauca.muisca.application.dto.AtlModuleResponse;
import com.unicauca.muisca.application.service.AtlModuleService;
import com.unicauca.muisca.domain.model.AtlModule;
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

/** Transformaciones modelo-a-modelo en ATL, compiladas en el servidor (MODEL_ENGINEER y ADMIN). */
@RestController
@RequestMapping("/api/atl-modules")
public class AtlModuleController {

    private final AtlModuleService atlModuleService;

    public AtlModuleController(AtlModuleService atlModuleService) {
        this.atlModuleService = atlModuleService;
    }

    @GetMapping
    public List<AtlModuleResponse> list() {
        return atlModuleService.list();
    }

    @GetMapping("/{id}")
    public AtlModuleResponse get(@PathVariable Long id) {
        return atlModuleService.get(id);
    }

    @GetMapping("/{id}/source")
    public ResponseEntity<byte[]> downloadSource(@PathVariable Long id) {
        AtlModule module = atlModuleService.find(id);
        return Files.download(module.getModuleName() + ".atl", module.getSourceCode().getBytes(StandardCharsets.UTF_8),
                Files.TEXT);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AtlModuleResponse create(@RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Long sourceMetamodelId,
            @RequestParam Long targetMetamodelId,
            @RequestParam MultipartFile atlFile,
            @CurrentUser UserEntity actor) {
        return atlModuleService.create(name, description, sourceMetamodelId, targetMetamodelId,
                Files.upload(atlFile), actor);
    }

    @PutMapping(path = "/{id}/source", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AtlModuleResponse updateSource(@PathVariable Long id, @RequestParam MultipartFile atlFile,
            @CurrentUser UserEntity actor) {
        return atlModuleService.updateSource(id, Files.upload(atlFile), actor);
    }

    /** Cambia nombre y descripción (el archivo no se modifica). */
    @PatchMapping("/{id}")
    public AtlModuleResponse updateMetadata(@PathVariable Long id, @Valid @RequestBody MetadataRequest request,
            @CurrentUser UserEntity actor) {
        return atlModuleService.updateMetadata(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser UserEntity actor) {
        atlModuleService.delete(id, actor);
    }
}
