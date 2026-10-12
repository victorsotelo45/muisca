package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.Execution;
import com.unicauca.muisca.domain.model.ExecutionStage;
import com.unicauca.muisca.domain.model.Transformation;
import com.unicauca.muisca.domain.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ExecutionRepository extends JpaRepository<Execution, Long>, ExecutionSearchRepository {

    String SUMMARY = "select new com.unicauca.muisca.domain.repository.ExecutionSummary("
            + "e.id, t.id, e.transformationName, u.username, e.status, e.stage, e.inputModelName, e.errorMessage, "
            + "e.durationMs, e.createdAt) from Execution e left join e.transformation t join e.executedBy u ";

    @Query(SUMMARY + "order by e.createdAt desc")
    List<ExecutionSummary> findAllSummaries();

    @Query(SUMMARY + "where u = :user order by e.createdAt desc")
    List<ExecutionSummary> findSummariesByUser(UserEntity user);

    boolean existsByExecutedBy(UserEntity user);

    @Modifying
    @Query("delete from Execution e where e.executedBy = :user")
    void deleteByExecutedBy(UserEntity user);

    @Modifying
    @Query("update Execution e set e.transformation = null where e.transformation = :transformation")
    void detachFromTransformation(Transformation transformation);

    /** Actualiza solo la etapa, sin reescribir los archivos de la ejecución. */
    @Transactional
    @Modifying
    @Query("update Execution e set e.stage = :stage where e.id = :id")
    void updateStage(Long id, ExecutionStage stage);

    /** Al arrancar, las ejecuciones que quedaron en curso por un reinicio se marcan como fallidas. */
    @Transactional
    @Modifying
    @Query("update Execution e set e.status = com.unicauca.muisca.domain.model.ExecutionStatus.FAILED, "
            + "e.stage = null, e.errorMessage = :message where e.status = "
            + "com.unicauca.muisca.domain.model.ExecutionStatus.RUNNING")
    int failInterrupted(String message);
}
