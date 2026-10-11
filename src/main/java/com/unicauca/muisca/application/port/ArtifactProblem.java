package com.unicauca.muisca.application.port;

/** Error o advertencia de compilación, como los que muestra la vista "Problems" de Eclipse. */
public record ArtifactProblem(Severity severity, Integer line, String message) {

    public enum Severity {
        ERROR, WARNING
    }

    public static ArtifactProblem error(Integer line, String message) {
        return new ArtifactProblem(Severity.ERROR, line, message);
    }

    public static ArtifactProblem warning(Integer line, String message) {
        return new ArtifactProblem(Severity.WARNING, line, message);
    }

    public boolean isError() {
        return severity == Severity.ERROR;
    }
}
