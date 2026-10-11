package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.AtlCompilationResponse;
import com.unicauca.muisca.application.dto.TemplateCompilationResponse;
import com.unicauca.muisca.application.service.WorkspaceService;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Espacio de trabajo del ingeniero de modelos (MODEL_ENGINEER y ADMIN): compilar y probar
 * módulos ATL y plantillas Acceleo con sus propios metamodelos y modelos, como en Eclipse,
 * sin registrarlos en el catálogo.
 */
@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping(path = "/atl/compile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AtlCompilationResponse compileAtl(@RequestParam MultipartFile atlFile) {
        return AtlCompilationResponse.from(workspaceService.compileAtl(Files.upload(atlFile)));
    }

    /** Devuelve el modelo de salida (.xmi). */
    @PostMapping(path = "/atl/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> runAtl(@RequestParam MultipartFile sourceMetamodel,
            @RequestParam MultipartFile targetMetamodel,
            @RequestParam MultipartFile atlFile,
            @RequestParam MultipartFile inputModel,
            @CurrentUser UserEntity actor) {
        byte[] output = workspaceService.runAtl(Files.upload(sourceMetamodel), Files.upload(targetMetamodel),
                Files.upload(atlFile), Files.upload(inputModel), actor);
        return Files.download(baseName(inputModel.getOriginalFilename()) + "-output.xmi",
                output, Files.XML);
    }

    @PostMapping(path = "/acceleo/compile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TemplateCompilationResponse compileTemplate(@RequestParam MultipartFile metamodel,
            @RequestParam MultipartFile templateFile,
            @CurrentUser UserEntity actor) {
        return TemplateCompilationResponse.from(
                workspaceService.compileTemplate(Files.upload(metamodel), Files.upload(templateFile), actor));
    }

    /** Devuelve un ZIP con el código generado. */
    @PostMapping(path = "/acceleo/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> runTemplate(@RequestParam MultipartFile metamodel,
            @RequestParam MultipartFile templateFile,
            @RequestParam MultipartFile inputModel,
            @CurrentUser UserEntity actor) {
        byte[] zip = workspaceService.runTemplate(Files.upload(metamodel), Files.upload(templateFile),
                Files.upload(inputModel), actor);
        return Files.download(baseName(inputModel.getOriginalFilename()) + "-codigo.zip", zip,
                Files.ZIP);
    }

    private static String baseName(String filename) {
        String name = filename == null ? "modelo" : filename;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
