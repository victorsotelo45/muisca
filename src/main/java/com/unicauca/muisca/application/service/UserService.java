package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.CreateUserRequest;
import com.unicauca.muisca.application.dto.UpdateUserRequest;
import com.unicauca.muisca.application.dto.UserResponse;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(new UserEntity(username, null, role)));
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
                    + "transformaciones o ejecuciones registradas; cámbiele el rol en lugar de eliminarlo");
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
