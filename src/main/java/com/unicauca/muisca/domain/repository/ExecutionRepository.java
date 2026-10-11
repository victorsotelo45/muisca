package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.Execution;
import com.unicauca.muisca.domain.model.Transformation;
import com.unicauca.muisca.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    String SUMMARY = "select new com.unicauca.muisca.domain.repository.ExecutionSummary("
            + "e.id, t.id, e.transformationName, u.username, e.status, e.inputModelName, e.errorMessage, "
            + "e.durationMs, e.createdAt) from Execution e left join e.transformation t join e.executedBy u ";

    @Query(SUMMARY + "order by e.createdAt desc")
    List<ExecutionSummary> findAllSummaries();

    @Query(SUMMARY + "where u = :user order by e.createdAt desc")
    List<ExecutionSummary> findSummariesByUser(UserEntity user);

    boolean existsByExecutedBy(UserEntity user);

    @Modifying
    @Query("update Execution e set e.transformation = null where e.transformation = :transformation")
    void detachFromTransformation(Transformation transformation);
}
