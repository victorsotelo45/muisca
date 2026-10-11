package com.unicauca.muisca.application.dto;

/** @param password confirmación; obligatoria si la cuenta tiene contraseña local */
public record DeleteAccountRequest(String password) {
}
