package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.ExecutionStatus;
import com.unicauca.muisca.domain.model.UserEntity;

import java.util.List;

/** Búsqueda paginada del historial con filtros opcionales (fragmento de ExecutionRepository). */
public interface ExecutionSearchRepository {

    /** Una página de resultados y el total de coincidencias. */
    record Page(List<ExecutionSummary> items, long total) {
    }

    /**
     * @param executedBy solo las de este usuario; null para todas (administrador)
     * @param status     null para cualquier estado
     * @param text       busca en transformación, modelo de entrada y usuario; null o vacío para no filtrar
     */
    Page search(UserEntity executedBy, ExecutionStatus status, String text, int page, int size);
}
