package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.application.port.ModelTransformationEngine.AtlCompilation;
import com.unicauca.muisca.domain.model.AtlCompiler;

import java.util.List;

public record AtlCompilationResponse(boolean valid, AtlCompiler compiler, String moduleName, String inModelName,
        String sourceAlias, String outModelName, String targetAlias, List<ArtifactProblem> problems) {

    public static AtlCompilationResponse from(AtlCompilation c) {
        return new AtlCompilationResponse(!c.hasErrors(), c.compiler(), c.moduleName(), c.inModelName(),
                c.sourceAlias(), c.outModelName(), c.targetAlias(), c.problems());
    }
}
