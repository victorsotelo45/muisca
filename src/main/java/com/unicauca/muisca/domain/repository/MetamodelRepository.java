package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.Metamodel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetamodelRepository extends JpaRepository<Metamodel, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByNsUri(String nsUri);

    Optional<Metamodel> findByNsUri(String nsUri);

    List<Metamodel> findAllByOrderByNameAsc();
}
