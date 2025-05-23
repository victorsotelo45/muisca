package com.unicauca.muisca.infraestructure.web;

import com.unicauca.muisca.application.service.UserService;
import com.unicauca.muisca.domain.dto.AuthRequest;
import com.unicauca.muisca.domain.dto.AuthResponse;
import com.unicauca.muisca.domain.dto.RegisterRequest;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infraestructure.config.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()));

            UserEntity user = userService.findByUsername(authRequest.getUsername())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            String role = user.getRole();
            String token = jwtUtil.generateToken(authRequest.getUsername(), role);
            return new AuthResponse(token);

        } catch (AuthenticationException e) {
            throw new RuntimeException("Credenciales inválidas");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        try {
            UserEntity user = userService.registerUser(request.getUsername(), request.getPassword(), "USER");
            return ResponseEntity.ok("Usuario registrado con éxito: " + user.getUsername());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Token inválido o expirado");
        }
        String username = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_USER");
        return ResponseEntity.ok(
                java.util.Map.of(
                        "username", username,
                        "role", role));
    }
}
