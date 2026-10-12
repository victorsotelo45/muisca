package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.ExecutionResponse;
import com.unicauca.muisca.application.dto.PublicationRequest;
import com.unicauca.muisca.application.dto.TransformationRequest;
import com.unicauca.muisca.application.dto.TransformationResponse;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.service.ExecutionService;
import com.unicauca.muisca.application.service.StoredModelService;
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
    private final StoredModelService storedModelService;

    public TransformationController(TransformationService transformationService, ExecutionService executionService,
            StoredModelService storedModelService) {
        this.transformationService = transformationService;
        this.executionService = executionService;
        this.storedModelService = storedModelService;
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
     * El usuario sube su modelo (inputModel) o elige uno de su biblioteca (modelId). Sin async
     * espera el resultado: 201 si terminó bien y 422 si el motor falló (ambas quedan en el
     * historial). Con async=true responde 202 con la ejecución RUNNING, que el cliente consulta en
     * GET /api/executions/{id} hasta que termine.
     */
    @PostMapping(path = "/{id}/executions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExecutionResponse> execute(@PathVariable Long id,
            @RequestParam(required = false) MultipartFile inputModel,
            @RequestParam(required = false) Long modelId,
            @RequestParam(defaultValue = "false") boolean async,
            @CurrentUser UserEntity actor) {
        if ((inputModel == null) == (modelId == null)) {
            throw new IllegalArgumentException("Envíe el archivo del modelo (inputModel) o el id de un modelo "
                    + "guardado (modelId), pero no ambos");
        }
        UploadedFile model = inputModel != null ? Files.upload(inputModel)
                : storedModelService.asUploadedFile(modelId, actor);
        if (async) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(executionService.start(id, model, actor));
        }
        ExecutionResponse execution = executionService.execute(id, model, actor);
        HttpStatus status = execution.status() == ExecutionStatus.SUCCEEDED ? HttpStatus.CREATED
                : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(execution);
    }
}
