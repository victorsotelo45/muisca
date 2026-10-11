package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.AtlModule;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.Transformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransformationRepository extends JpaRepository<Transformation, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByInputMetamodel(Metamodel metamodel);

    boolean existsByAtlModule(AtlModule atlModule);

    boolean existsByAcceleoModule(AcceleoModule acceleoModule);

    List<Transformation> findAllByOrderByNameAsc();

    List<Transformation> findByPublishedOrderByNameAsc(boolean published);
}
