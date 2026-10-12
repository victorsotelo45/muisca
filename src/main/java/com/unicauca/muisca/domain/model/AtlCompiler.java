package com.unicauca.muisca.domain.model;

/**
 * Compilador ATL con el que se compila un módulo, elegido igual que en Eclipse por la
 * anotación {@code -- @atlcompiler} de su cabecera. Cada uno se ejecuta en su propia VM.
 */
public enum AtlCompiler {
    /** Compila a .emftvm y se ejecuta en EMFTVM. */
    EMFTVM,
    /** Compilador por defecto de ATL: compila a .asm y se ejecuta en EMFVM. */
    ATL2006
}
