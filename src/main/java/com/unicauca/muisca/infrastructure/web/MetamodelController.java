package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.MetamodelResponse;
import com.unicauca.muisca.application.service.MetamodelService;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Metamodelos Ecore de las plataformas (MODEL_ENGINEER y ADMIN). */
@RestController
@RequestMapping("/api/metamodels")
public class MetamodelController {

    private final MetamodelService metamodelService;

    public MetamodelController(MetamodelService metamodelService) {
        this.metamodelService = metamodelService;
    }

    @GetMapping
    public List<MetamodelResponse> list() {
        return metamodelService.list();
    }

    @GetMapping("/{id}")
    public MetamodelResponse get(@PathVariable Long id) {
        return metamodelService.get(id);
    }

    @GetMapping("/{id}/ecore")
    public ResponseEntity<byte[]> downloadEcore(@PathVariable Long id) {
        Metamodel metamodel = metamodelService.find(id);
        return Files.download(metamodel.getEcoreFileName(), metamodel.getEcoreData(), Files.XML);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MetamodelResponse create(@RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile ecoreFile,
            @RequestParam(required = false) MultipartFile genmodelFile,
            @CurrentUser UserEntity actor) {
        return metamodelService.create(name, description, Files.upload(ecoreFile), Files.upload(genmodelFile), actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @CurrentUser UserEntity actor) {
        metamodelService.delete(id, actor);
    }
}
