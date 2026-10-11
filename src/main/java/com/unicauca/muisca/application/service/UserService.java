package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.CreateUserRequest;
import com.unicauca.muisca.application.dto.UpdateUserRequest;
import com.unicauca.muisca.application.dto.UserResponse;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.ForbiddenOperationException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.ExecutionRepository;
import com.unicauca.muisca.domain.repository.StoredModelRepository;
import com.unicauca.muisca.domain.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final String REGISTRATION_DISABLED = "El registro de cuentas nuevas está deshabilitado. "
            + "Solicite una cuenta a un administrador de MUISCA.";

    private final UserRepository userRepository;
    private final ExecutionRepository executionRepository;
    private final StoredModelRepository storedModelRepository;
    private final SettingsService settingsService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, ExecutionRepository executionRepository,
            StoredModelRepository storedModelRepository, SettingsService settingsService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.executionRepository = executionRepository;
        this.storedModelRepository = storedModelRepository;
        this.settingsService = settingsService;
        this.passwordEncoder = passwordEncoder;
    }

    /** Auto-registro público: siempre rol USER y solo si la configuración lo permite. */
    @Transactional
    public UserEntity selfRegister(String username, String password) {
        if (!settingsService.isRegistrationEnabled()) {
            throw new ForbiddenOperationException(REGISTRATION_DISABLED);
        }
        return register(username, password, Role.USER);
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAllByOrderByUsernameAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        return UserResponse.from(register(request.username(), request.password(), request.role()));
    }

    @Transactional
    public UserEntity register(String username, String password, Role role) {
        String normalized = username.trim();
        if (userRepository.existsByUsername(normalized)) {
            throw new ConflictException("El nombre de usuario '" + normalized + "' ya existe");
        }
        return userRepository.save(new UserEntity(normalized, passwordEncoder.encode(password), role));
    }

    /**
     * Registra (o recupera) una cuenta de un proveedor OAuth (Google). No tiene
     * contraseña local, así que no puede iniciar sesión por usuario/contraseña.
     */
    @Transactional
    public UserEntity registerOAuthUser(String username, Role role) {
        return userRepository.findByUsername(username).orElseGet(() -> {
            if (!settingsService.isRegistrationEnabled()) {
                throw new ForbiddenOperationException(REGISTRATION_DISABLED);
            }
            return userRepository.save(new UserEntity(username, null, role));
        });
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, UserEntity actor) {
        UserEntity user = find(id);
        if (request.username() != null && !request.username().isBlank()
                && !request.username().trim().equals(user.getUsername())) {
            String username = request.username().trim();
            if (userRepository.existsByUsername(username)) {
                throw new ConflictException("El nombre de usuario '" + username + "' ya existe");
            }
            user.setUsername(username);
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.role() != null && request.role() != user.getRole()) {
            if (user.getRole() == Role.ADMIN) {
                requireAnotherAdmin(user, actor, "quitarle el rol de administrador a");
            }
            user.setRole(request.role());
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id, UserEntity actor) {
        UserEntity user = find(id);
        if (user.getRole() == Role.ADMIN) {
            requireAnotherAdmin(user, actor, "eliminar a");
        }
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("El usuario '" + user.getUsername() + "' tiene metamodelos, módulos, "
                    + "transformaciones, modelos o ejecuciones registrados; cámbiele el rol en lugar de eliminarlo");
        }
    }

    /**
     * Cambio de la propia contraseña. Las cuentas que solo entran con Google no tienen contraseña
     * actual: al definir una, también podrán entrar con usuario y contraseña.
     */
    @Transactional
    public void changeOwnPassword(UserEntity actor, String currentPassword, String newPassword) {
        UserEntity user = find(actor.getId());
        requirePassword(user, currentPassword, "La contraseña actual no es correcta");
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Elimina la propia cuenta junto con su historial y su biblioteca de modelos. Los recursos del
     * catálogo (metamodelos, módulos, transformaciones) no se borran en cascada: si existen, la
     * operación se rechaza para no dejar el catálogo incompleto.
     */
    @Transactional
    public void deleteOwnAccount(UserEntity actor, String password) {
        UserEntity user = find(actor.getId());
        requirePassword(user, password, "La contraseña no es correcta");
        if (user.getRole() == Role.ADMIN) {
            throw new ConflictException("Un administrador no puede eliminar su propia cuenta; pida a otro "
                    + "administrador que lo haga");
        }
        executionRepository.deleteByExecutedBy(user);
        storedModelRepository.deleteByOwner(user);
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Su cuenta tiene metamodelos, módulos o transformaciones registrados en el "
                    + "catálogo; pida a un administrador que los reasigne o elimine antes de borrar la cuenta");
        }
    }

    private void requirePassword(UserEntity user, String password, String message) {
        if (user.getPassword() != null && (password == null || !passwordEncoder.matches(password, user.getPassword()))) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireAnotherAdmin(UserEntity target, UserEntity actor, String action) {
        if (target.getId().equals(actor.getId())) {
            throw new ConflictException("Un administrador no puede " + action + " su propia cuenta");
        }
        if (userRepository.countByRole(Role.ADMIN) <= 1) {
            throw new ConflictException("No se puede " + action + " el único administrador del sistema");
        }
    }

    private UserEntity find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + id));
    }
}
