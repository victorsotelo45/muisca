package com.unicauca.muisca.application.port;

/** Falla al ejecutar una transformación o generación sobre un modelo. */
public class ModelingException extends RuntimeException {

    public ModelingException(String message) {
        super(message);
    }

    public ModelingException(String message, Throwable cause) {
        super(message, cause);
    }
}
