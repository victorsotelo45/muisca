package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.application.port.CodeGenerationEngine.TemplateCompilation;

import java.util.List;

public record TemplateCompilationResponse(boolean valid, String moduleName, String mainTemplate,
        List<ArtifactProblem> problems) {

    public static TemplateCompilationResponse from(TemplateCompilation c) {
        return new TemplateCompilationResponse(!c.hasErrors(), c.moduleName(), c.mainTemplate(), c.problems());
    }
}
