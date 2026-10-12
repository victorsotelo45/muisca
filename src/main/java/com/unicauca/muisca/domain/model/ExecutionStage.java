package com.unicauca.muisca.domain.model;

/** Etapa de una ejecución en curso, para mostrar el progreso al usuario. */
public enum ExecutionStage {
    /** Esperando turno en la cola de ejecución. */
    QUEUED,
    /** Transformación modelo a modelo (ATL). */
    ATL,
    /** Generación de código (Acceleo). */
    ACCELEO
}
