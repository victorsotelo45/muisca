package com.unicauca.muisca.infraestructure.web;

import com.unicauca.muisca.application.service.AtlService;
import com.unicauca.muisca.domain.dto.AtlTransformRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atl")
public class AtlController {

    @Autowired
    private AtlService atlService;

    @PostMapping("/transform")
    public ResponseEntity<String> transform(@RequestBody AtlTransformRequest request) {

        String result = atlService.executeTransformation(
                request.getTransformationAsmPath(),
                request.getInputModelPath(),
                request.getInputMetamodelPath(),
                request.getOutputMetamodelPath(),
                request.getOutputModelPath(),
                request.getInputModelName(),
                request.getOutputModelName()
        );

        return ResponseEntity.ok(result);
    }
}
