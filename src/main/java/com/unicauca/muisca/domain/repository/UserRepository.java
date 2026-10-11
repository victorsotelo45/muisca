package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    Optional<UserEntity> findFirstByRole(Role role);

    List<UserEntity> findAllByOrderByUsernameAsc();
}
