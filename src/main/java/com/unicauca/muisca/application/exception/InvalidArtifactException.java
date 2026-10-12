package com.unicauca.muisca.application.exception;

import com.unicauca.muisca.application.port.ArtifactProblem;

import java.util.List;

/** Un archivo subido (metamodelo, módulo ATL, plantilla Acceleo o modelo) no es válido. */
public class InvalidArtifactException extends RuntimeException {

    private final List<ArtifactProblem> problems;

    public InvalidArtifactException(String message) {
        this(message, List.of());
    }

    public InvalidArtifactException(String message, List<ArtifactProblem> problems) {
        super(message);
        this.problems = problems;
    }

    public List<ArtifactProblem> getProblems() {
        return problems;
    }
}
