package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.exception.ForbiddenOperationException;
import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;

/** Reglas de autorización a nivel de recurso (las de nivel de ruta están en SecurityConfig). */
final class Permissions {

    private Permissions() {
    }

    static boolean isAdmin(UserEntity user) {
        return user.getRole() == Role.ADMIN;
    }

    static boolean canManageCatalog(UserEntity user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.MODEL_ENGINEER;
    }

    /** El administrador puede modificar cualquier recurso; el ingeniero de modelos, solo los suyos. */
    static void requireOwnerOrAdmin(UserEntity actor, UserEntity owner, String resource) {
        if (!isAdmin(actor) && !actor.getId().equals(owner.getId())) {
            throw new ForbiddenOperationException("Solo el creador de " + resource + " o un administrador puede "
                    + "modificarlo");
        }
    }
}
