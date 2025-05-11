package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.AtlFile;
import com.unicauca.muisca.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtlFileRepository extends JpaRepository<AtlFile, Long> {
    List<AtlFile> findByUser(UserEntity user);
}
