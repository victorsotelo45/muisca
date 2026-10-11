package com.unicauca.muisca.application.dto;

import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;

/** @param localAccount false si la cuenta solo inicia sesión con Google */
public record UserResponse(Long id, String username, Role role, boolean localAccount) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getPassword() != null);
    }
}
