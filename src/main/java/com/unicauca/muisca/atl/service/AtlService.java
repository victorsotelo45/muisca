package com.unicauca.muisca.atl.service;

import org.eclipse.m2m.atl.engine.emfvm.ASMInterpreter;
import org.eclipse.m2m.atl.engine.emfvm.ASMModule;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AtlService {
    private static final String TRANSFORMATION_PATH = "src/main/resources/transformations/";

    public String executeTransformation(String modelPath, String transformationName) {
        try {
            // Cargar el módulo ATL
            ASMModule module = new ASMModule();
            File transformationFile = new File(TRANSFORMATION_PATH + transformationName + ".asm");
            module.load(transformationFile);

            // Configurar el intérprete
            ASMInterpreter interpreter = new ASMInterpreter(module);
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("model", new File(modelPath));

            // Ejecutar transformación
            interpreter.execute(inputs);

            return "Transformación ejecutada exitosamente.";
        } catch (Exception e) {
            return "Error en la transformación: " + e.getMessage();
        }
    }
}
