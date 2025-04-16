package com.unicauca.muisca.infraestructure.web;

import com.unicauca.muisca.application.service.AtlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atl")
public class AtlController {

    // Rutas predefinidas para los modelos y metamodelos
    public final static String IN_METAMODEL = "src/main/resources/atl/Metamodels/HyperledgerGo/hyperledgerGo.ecore";
    public final static String OUT_METAMODEL = "src/main/resources/atl/Metamodels/HyperledgerJava/hyperledgerJava.ecore";
    public final static String IN_MODEL = "src/main/resources/atl/Models/SCGo.xmi";
    public final static String OUT_MODEL = "src/main/resources/atl/Models/SCJavaTest.xmi";
    public final static String TRANSFORMATION_DIR = "src/main/resources/atl/Transformations/ATL/";
    public final static String TRANSFORMATION_MODULE = "Go2Java";

    @Autowired
    private AtlService atlService;

    @PostMapping("/transform")
    public ResponseEntity<?> transform() {
        try {
            // Registrar los metamodelos
            atlService.registerInputMetamodel(IN_METAMODEL);
            atlService.registerOutputMetamodel(OUT_METAMODEL);

            // Ejecutar la transformación
            atlService.launch(
                IN_METAMODEL,
                IN_MODEL,
                OUT_METAMODEL,
                OUT_MODEL,
                TRANSFORMATION_DIR,
                TRANSFORMATION_MODULE
            );

            // Devolver respuesta exitosa
            return ResponseEntity.ok("Transformación completada exitosamente. Modelo de salida: " + OUT_MODEL);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error durante la transformación: " + e.getMessage());
        }
    }
}