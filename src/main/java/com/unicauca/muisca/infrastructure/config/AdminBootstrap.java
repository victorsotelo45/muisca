package com.unicauca.muisca.infrastructure.config;

import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea el primer usuario ADMIN a partir de las variables de entorno ADMIN_USERNAME /
 * ADMIN_PASSWORD si todavía no existe ningún administrador. Sin esto, un despliegue
 * limpio no tiene forma de alcanzar los endpoints /api/users (protegidos con
 * hasRole('ADMIN')), ya que el auto-registro público siempre crea usuarios con rol USER.
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${admin.bootstrap.username:}") String bootstrapUsername,
            @Value("${admin.bootstrap.password:}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            log.warn("No existe ningún usuario ADMIN y no se definieron ADMIN_USERNAME/ADMIN_PASSWORD: "
                    + "los endpoints /api/users (y el alta de metamodelos/transformaciones) quedarán "
                    + "inalcanzables hasta crear o promover un administrador manualmente en la base de datos.");
            return;
        }
        if (userRepository.findByUsername(bootstrapUsername).isPresent()) {
            log.warn("ADMIN_USERNAME='{}' ya existe pero no tiene rol ADMIN; no se modifica automáticamente.",
                    bootstrapUsername);
            return;
        }
        UserEntity admin = new UserEntity();
        admin.setUsername(bootstrapUsername);
        admin.setPassword(passwordEncoder.encode(bootstrapPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Usuario ADMIN inicial creado: {}", bootstrapUsername);
    }
}
