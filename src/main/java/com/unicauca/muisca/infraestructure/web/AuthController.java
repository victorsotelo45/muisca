package com.unicauca.muisca.infraestructure.web;

import com.unicauca.muisca.domain.dto.AuthRequest;
import com.unicauca.muisca.domain.dto.AuthResponse;
import com.unicauca.muisca.infraestructure.config.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest authRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()));

            String token = jwtUtil.generateToken(authRequest.getUsername());
            return new AuthResponse(token);

        } catch (AuthenticationException e) {
            throw new RuntimeException("Credenciales inválidas");
        }
    }
}
