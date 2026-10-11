package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.Metamodel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetamodelRepository extends JpaRepository<Metamodel, Long> {

    boolean existsByName(String name);

    boolean existsByNsUri(String nsUri);

    List<Metamodel> findAllByOrderByNameAsc();
}
