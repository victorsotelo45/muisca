package com.unicauca.muisca.domain.repository;

import com.unicauca.muisca.domain.model.ExecutionStatus;
import com.unicauca.muisca.domain.model.UserEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * La consulta se arma solo con los filtros presentes: PostgreSQL no admite condiciones del tipo
 * ":param is null" cuando el parámetro no tiene tipo, y así la misma JPQL sirve en MySQL y PostgreSQL.
 */
class ExecutionSearchRepositoryImpl implements ExecutionSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page search(UserEntity executedBy, ExecutionStatus status, String text, int page, int size) {
        List<String> conditions = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (executedBy != null) {
            conditions.add("u = :user");
            parameters.put("user", executedBy);
        }
        if (status != null) {
            conditions.add("e.status = :status");
            parameters.put("status", status);
        }
        if (text != null && !text.isBlank()) {
            conditions.add("(lower(e.transformationName) like :text or lower(e.inputModelName) like :text "
                    + "or lower(u.username) like :text)");
            parameters.put("text", "%" + text.trim().toLowerCase(Locale.ROOT) + "%");
        }
        String where = conditions.isEmpty() ? "" : "where " + String.join(" and ", conditions) + " ";

        TypedQuery<ExecutionSummary> query = entityManager.createQuery(
                ExecutionRepository.SUMMARY + where + "order by e.createdAt desc, e.id desc", ExecutionSummary.class);
        TypedQuery<Long> count = entityManager.createQuery(
                "select count(e) from Execution e join e.executedBy u " + where, Long.class);
        parameters.forEach((name, value) -> {
            query.setParameter(name, value);
            count.setParameter(name, value);
        });
        List<ExecutionSummary> items = query.setFirstResult(page * size).setMaxResults(size).getResultList();
        return new Page(items, count.getSingleResult());
    }
}
