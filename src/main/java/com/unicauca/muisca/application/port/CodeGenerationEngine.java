package com.unicauca.muisca.application.port;

import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.Metamodel;

import java.util.List;
import java.util.Map;

/** Motor de generación modelo-a-texto (Acceleo), ejecutado en el servidor. */
public interface CodeGenerationEngine {

    record TemplateCompilation(String moduleName, String mainTemplate, List<ArtifactProblem> problems) {

        public boolean hasErrors() {
            return problems.stream().anyMatch(ArtifactProblem::isError);
        }
    }

    TemplateCompilation compile(String templateSource, Metamodel metamodel);

    /** Genera el código a partir del modelo; devuelve ruta relativa → contenido de cada archivo. */
    Map<String, byte[]> generate(AcceleoModule module, byte[] model);
}
