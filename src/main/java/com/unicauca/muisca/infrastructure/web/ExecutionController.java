package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.ExecutionResponse;
import com.unicauca.muisca.application.dto.GeneratedFile;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.service.ExecutionService;
import com.unicauca.muisca.domain.model.Execution;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.ExecutionSummary;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Historial de ejecuciones: cada usuario ve las suyas y el administrador, todas. */
@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    public List<ExecutionSummary> list(@CurrentUser UserEntity actor) {
        return executionService.list(actor);
    }

    @GetMapping("/{id}")
    public ExecutionResponse get(@PathVariable Long id, @CurrentUser UserEntity actor) {
        return executionService.get(id, actor);
    }

    @GetMapping("/{id}/input-model")
    public ResponseEntity<byte[]> inputModel(@PathVariable Long id, @CurrentUser UserEntity actor) {
        Execution execution = executionService.find(id, actor);
        return Files.download(execution.getInputModelName(), execution.getInputModel(), Files.XML);
    }

    @GetMapping("/{id}/output-model")
    public ResponseEntity<byte[]> outputModel(@PathVariable Long id, @CurrentUser UserEntity actor) {
        Execution execution = executionService.find(id, actor);
        if (execution.getOutputModel() == null) {
            throw new ResourceNotFoundException("La ejecución " + id + " no produjo un modelo de salida");
        }
        return Files.download(execution.getOutputModelName(), execution.getOutputModel(), Files.XML);
    }

    @GetMapping("/{id}/generated-code")
    public ResponseEntity<byte[]> generatedCode(@PathVariable Long id, @CurrentUser UserEntity actor) {
        Execution execution = executionService.find(id, actor);
        if (execution.getGeneratedCode() == null) {
            throw new ResourceNotFoundException("La ejecución " + id + " no generó código");
        }
        return Files.download("ejecucion-" + id + "-codigo.zip", execution.getGeneratedCode(), Files.ZIP);
    }

    @GetMapping("/{id}/generated-files")
    public List<GeneratedFile> generatedFiles(@PathVariable Long id, @CurrentUser UserEntity actor) {
        return executionService.generatedFiles(id, actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser UserEntity actor) {
        executionService.delete(id, actor);
    }
}
