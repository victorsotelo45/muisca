package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.MetadataRequest;
import com.unicauca.muisca.application.dto.StoredModelResponse;
import com.unicauca.muisca.application.service.StoredModelService;
import com.unicauca.muisca.domain.model.StoredModel;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Biblioteca de modelos de cada usuario (cualquier rol autenticado). */
@RestController
@RequestMapping("/api/models")
public class StoredModelController {

    private final StoredModelService storedModelService;

    public StoredModelController(StoredModelService storedModelService) {
        this.storedModelService = storedModelService;
    }

    @GetMapping
    public List<StoredModelResponse> list(@CurrentUser UserEntity actor) {
        return storedModelService.list(actor);
    }

    @GetMapping("/{id}")
    public StoredModelResponse get(@PathVariable Long id, @CurrentUser UserEntity actor) {
        return storedModelService.get(id, actor);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long id, @CurrentUser UserEntity actor) {
        StoredModel model = storedModelService.find(id, actor);
        return Files.download(model.getFileName(), model.getContent(), Files.XML);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StoredModelResponse create(@RequestParam MultipartFile modelFile,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @CurrentUser UserEntity actor) {
        return storedModelService.create(name, description, Files.upload(modelFile), actor);
    }

    @PostMapping("/from-execution/{executionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public StoredModelResponse createFromExecution(@PathVariable Long executionId,
            @RequestParam(required = false) String name, @CurrentUser UserEntity actor) {
        return storedModelService.createFromExecution(executionId, name, actor);
    }

    @PatchMapping("/{id}")
    public StoredModelResponse updateMetadata(@PathVariable Long id, @Valid @RequestBody MetadataRequest request,
            @CurrentUser UserEntity actor) {
        return storedModelService.updateMetadata(id, request, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser UserEntity actor) {
        storedModelService.delete(id, actor);
    }
}
