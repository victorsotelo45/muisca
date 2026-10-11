package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.ExecutionResponse;
import com.unicauca.muisca.application.dto.PublicationRequest;
import com.unicauca.muisca.application.dto.TransformationRequest;
import com.unicauca.muisca.application.dto.TransformationResponse;
import com.unicauca.muisca.application.service.ExecutionService;
import com.unicauca.muisca.application.service.TransformationService;
import com.unicauca.muisca.domain.model.ExecutionStatus;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Catálogo de transformaciones que ve el usuario final. Cualquier usuario autenticado lista
 * y ejecuta las publicadas; MODEL_ENGINEER/ADMIN las definen y solo ADMIN las publica.
 */
@RestController
@RequestMapping("/api/transformations")
public class TransformationController {

    private final TransformationService transformationService;
    private final ExecutionService executionService;

    public TransformationController(TransformationService transformationService, ExecutionService executionService) {
        this.transformationService = transformationService;
        this.executionService = executionService;
    }

    @GetMapping
    public List<TransformationResponse> list(@RequestParam(required = false) Boolean published,
            @CurrentUser UserEntity actor) {
        return transformationService.list(actor, published);
    }

    @GetMapping("/{id}")
    public TransformationResponse get(@PathVariable Long id, @CurrentUser UserEntity actor) {
        return transformationService.get(id, actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransformationResponse create(@Valid @RequestBody TransformationRequest request,
            @CurrentUser UserEntity actor) {
        return transformationService.create(request, actor);
    }

    @PutMapping("/{id}")
    public TransformationResponse update(@PathVariable Long id, @Valid @RequestBody TransformationRequest request,
            @CurrentUser UserEntity actor) {
        return transformationService.update(id, request, actor);
    }

    @PatchMapping("/{id}/publication")
    public TransformationResponse setPublication(@PathVariable Long id, @Valid @RequestBody PublicationRequest request,
            @CurrentUser UserEntity actor) {
        return transformationService.setPublished(id, request.published(), actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser UserEntity actor) {
        transformationService.delete(id, actor);
    }

    /**
     * El usuario sube su modelo y obtiene el resultado. Responde 201 si la ejecución terminó
     * bien y 422 si el motor falló; en ambos casos la ejecución queda en el historial.
     */
    @PostMapping(path = "/{id}/executions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExecutionResponse> execute(@PathVariable Long id, @RequestParam MultipartFile inputModel,
            @CurrentUser UserEntity actor) {
        ExecutionResponse execution = executionService.execute(id, Files.upload(inputModel), actor);
        HttpStatus status = execution.status() == ExecutionStatus.SUCCEEDED ? HttpStatus.CREATED
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(execution);
    }
}
