package com.unicauca.muisca.infraestructure.web;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.unicauca.muisca.application.service.UserService;
import com.unicauca.muisca.domain.dto.TokenDto;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.infraestructure.config.jwt.JwtUtil;

@RestController
@RequestMapping("/oauth")
public class OauthController {

    @Value("${google.client.id}")
    private String googleClientId;

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public OauthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/google")
    public ResponseEntity<?> google(@RequestBody TokenDto tokenDto)
            throws IOException, java.security.GeneralSecurityException {
        final NetHttpTransport transport = new NetHttpTransport();
        final GsonFactory gsonFactory = GsonFactory.getDefaultInstance();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(transport, gsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        final GoogleIdToken googleIdToken = verifier.verify(tokenDto.getValue());
        if (googleIdToken != null) {
            final GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();

            UserEntity user = userService.findByUsername(email)
                    .orElseGet(() -> userService.registerUser(email, "", "USER"));

            String jwt = jwtUtil.generateToken(user.getUsername(), user.getRole());

            return ResponseEntity.ok(java.util.Map.of("token", jwt));
        } else {
            return ResponseEntity.badRequest().body("Token de Google inválido");
        }
    }

    @PostMapping("/facebook")
    public ResponseEntity<?> facebook(@RequestBody TokenDto tokenDto) {
        Facebook facebook = new FacebookTemplate(tokenDto.getValue());
        String[] fields = { "id", "email", "name" };
        org.springframework.social.facebook.api.User fbUser = facebook.fetchObject("me",
                org.springframework.social.facebook.api.User.class, fields);

        String email = fbUser.getEmail();
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body("No se pudo obtener el email de Facebook.");
        }

        UserEntity user = userService.findByUsername(email)
                .orElseGet(() -> userService.registerUser(email, "", "USER"));

        String jwt = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return ResponseEntity.ok(java.util.Map.of("token", jwt));
    }
}