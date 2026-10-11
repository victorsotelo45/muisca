package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.Metamodel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcceleoModuleRepository extends JpaRepository<AcceleoModule, Long> {

    boolean existsByName(String name);

    boolean existsByMetamodel(Metamodel metamodel);

    List<AcceleoModule> findAllByOrderByNameAsc();
}
