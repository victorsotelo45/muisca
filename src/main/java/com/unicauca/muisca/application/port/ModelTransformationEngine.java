package com.unicauca.muisca.application.port;

import com.unicauca.muisca.domain.model.AtlCompiler;
import com.unicauca.muisca.domain.model.AtlModule;

import java.util.List;

/** Motor de transformaciones modelo-a-modelo (ATL), ejecutado en el servidor. */
public interface ModelTransformationEngine {

    record AtlCompilation(AtlCompiler compiler, String moduleName, String inModelName, String sourceAlias,
            String outModelName, String targetAlias, byte[] compiledModule, List<ArtifactProblem> problems) {

        public boolean hasErrors() {
            return problems.stream().anyMatch(ArtifactProblem::isError);
        }
    }

    AtlCompilation compile(String atlSource);

    /** Ejecuta el módulo compilado sobre el modelo de entrada y devuelve el modelo de salida (XMI). */
    byte[] transform(AtlModule module, byte[] inputModel);
}
