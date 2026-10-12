package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.ChangePasswordRequest;
import com.unicauca.muisca.application.dto.DeleteAccountRequest;
import com.unicauca.muisca.application.dto.UserResponse;
import com.unicauca.muisca.application.service.UserService;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Autogestión de la propia cuenta (cualquier rol autenticado). */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@CurrentUser UserEntity actor) {
        return UserResponse.from(actor);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request, @CurrentUser UserEntity actor) {
        userService.changeOwnPassword(actor, request.currentPassword(), request.newPassword());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@RequestBody(required = false) DeleteAccountRequest request,
            @CurrentUser UserEntity actor) {
        userService.deleteOwnAccount(actor, request == null ? null : request.password());
    }
}
