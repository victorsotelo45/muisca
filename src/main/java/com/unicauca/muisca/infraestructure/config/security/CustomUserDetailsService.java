package com.unicauca.muisca.infraestructure.config.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Aquí puedes cargar el usuario desde una base de datos o cualquier otra fuente
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        if ("admin".equals(username)) {
            return User.builder()
                    .username("admin")
                    .password(encoder.encode("admin123")) // Contraseña sin cifrar (solo para pruebas)
                    .roles("ADMIN") // Rol del usuario
                    .build();
        } else if ("user".equals(username)) {
            return User.builder()
                    .username("user")
                    .password(encoder.encode("admin123")) // Contraseña sin cifrar (solo para pruebas)
                    .roles("USER") // Rol del usuario
                    .build();
        } else {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
    }
}