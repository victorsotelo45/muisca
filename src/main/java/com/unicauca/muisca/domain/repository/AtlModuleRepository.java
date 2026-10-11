package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.AtlModule;
import com.unicauca.muisca.domain.model.Metamodel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtlModuleRepository extends JpaRepository<AtlModule, Long> {

    boolean existsByName(String name);

    boolean existsBySourceMetamodelOrTargetMetamodel(Metamodel source, Metamodel target);

    List<AtlModule> findAllByOrderByNameAsc();
}
