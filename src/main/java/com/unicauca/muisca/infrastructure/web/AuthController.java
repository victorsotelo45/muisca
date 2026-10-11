package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.AuthRequest;
import com.unicauca.muisca.application.dto.AuthResponse;
import com.unicauca.muisca.application.dto.RegisterRequest;
import com.unicauca.muisca.application.dto.UserResponse;
import com.unicauca.muisca.application.service.UserService;
import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infrastructure.security.CurrentUser;
import com.unicauca.muisca.infrastructure.security.JwtUtil;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserEntity user = userService.findByUsername(request.username()).orElseThrow();
        return new AuthResponse(jwtUtil.generateToken(user.getUsername(), user.getRole().name()));
    }

    /** El auto-registro siempre crea cuentas USER; los demás roles los asigna un administrador. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(userService.register(request.username(), request.password(), Role.USER));
    }

    @GetMapping("/validate")
    public UserResponse validate(@CurrentUser UserEntity user) {
        return UserResponse.from(user);
    }
}
