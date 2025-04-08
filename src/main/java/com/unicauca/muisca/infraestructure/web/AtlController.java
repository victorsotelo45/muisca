package com.unicauca.muisca.infraestructure.web;

import com.unicauca.muisca.application.service.AtlService;
import com.unicauca.muisca.domain.dto.AtlTransformRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/atl")
public class AtlController {

    @Autowired
    private AtlService atlService;

    @PostMapping("/transform")
    public ResponseEntity<?> transform(
        @RequestParam("inputModel") MultipartFile inputModel,
        @RequestParam("inputMetamodel") MultipartFile inputMetamodel,
        @RequestParam("outputMetamodel") MultipartFile outputMetamodel
    ) {

        String result = atlService.executeTransformation(
            "C:/Users/VictorManuelSoteloMa/Documents/Tesis/web_project/muiscaweb/src/main/java/com/unicauca/muisca/atl/Transformations/ATL/Go2Java.asm", // transformationAsmPath
            inputModel, // inputModelPath
            inputMetamodel, // inputMetamodelPath
            outputMetamodel, // outputMetamodelPath
            "C:/Users/VictorManuelSoteloMa/Documents/Tesis/web_project/muiscaweb/src/main/java/com/unicauca/muisca/atl/Models/resultadoTransformacion.xmi", // outputModelPath
            "Go", // inputModelName
            "Java" // outputModelName
        );

        return ResponseEntity.ok(result);
    }
}
