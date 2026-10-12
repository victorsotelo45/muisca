package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.StoredModel;
import com.unicauca.muisca.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StoredModelRepository extends JpaRepository<StoredModel, Long> {

    List<StoredModel> findByOwnerOrderByNameAsc(UserEntity owner);

    List<StoredModel> findAllByOrderByNameAsc();

    boolean existsByOwnerAndName(UserEntity owner, String name);

    boolean existsByOwnerAndNameAndIdNot(UserEntity owner, String name, Long id);

    boolean existsByMetamodel(Metamodel metamodel);

    @Modifying
    @Query("delete from StoredModel m where m.owner = :owner")
    void deleteByOwner(UserEntity owner);
}
