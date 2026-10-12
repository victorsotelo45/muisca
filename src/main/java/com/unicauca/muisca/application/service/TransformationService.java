package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.TransformationRequest;
import com.unicauca.muisca.application.dto.TransformationResponse;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.ForbiddenOperationException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.AtlModule;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.Transformation;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.ExecutionRepository;
import com.unicauca.muisca.domain.repository.TransformationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Catálogo de transformaciones. Ingenieros de modelos y administradores las definen
 * encadenando módulos ATL/Acceleo; solo el administrador las publica, y el rol USER solo
 * ve y ejecuta las publicadas.
 */
@Service
public class TransformationService {

    private final TransformationRepository transformationRepository;
    private final ExecutionRepository executionRepository;
    private final MetamodelService metamodelService;
    private final AtlModuleService atlModuleService;
    private final AcceleoModuleService acceleoModuleService;

    public TransformationService(TransformationRepository transformationRepository,
            ExecutionRepository executionRepository, MetamodelService metamodelService,
            AtlModuleService atlModuleService, AcceleoModuleService acceleoModuleService) {
        this.transformationRepository = transformationRepository;
        this.executionRepository = executionRepository;
        this.metamodelService = metamodelService;
        this.atlModuleService = atlModuleService;
        this.acceleoModuleService = acceleoModuleService;
    }

    /** @param published filtro opcional; el rol USER siempre recibe solo las publicadas */
    @Transactional(readOnly = true)
    public List<TransformationResponse> list(UserEntity actor, Boolean published) {
        List<Transformation> transformations;
        if (!Permissions.canManageCatalog(actor)) {
            transformations = transformationRepository.findByPublishedOrderByNameAsc(true);
        } else if (published != null) {
            transformations = transformationRepository.findByPublishedOrderByNameAsc(published);
        } else {
            transformations = transformationRepository.findAllByOrderByNameAsc();
        }
        return transformations.stream().map(TransformationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransformationResponse get(Long id, UserEntity actor) {
        return TransformationResponse.from(findVisible(id, actor));
    }

    /** Las transformaciones sin publicar no existen para el rol USER. */
    @Transactional(readOnly = true)
    public Transformation findVisible(Long id, UserEntity actor) {
        Transformation transformation = transformationRepository.findById(id)
                .filter(t -> t.isPublished() || Permissions.canManageCatalog(actor))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la transformación con id " + id));
        return transformation;
    }

    @Transactional
    public TransformationResponse create(TransformationRequest request, UserEntity actor) {
        if (transformationRepository.existsByName(request.name())) {
            throw new ConflictException("Ya existe una transformación con el nombre '" + request.name() + "'");
        }
        Transformation transformation = new Transformation(actor);
        define(transformation, request);
        return TransformationResponse.from(transformationRepository.save(transformation));
    }

    @Transactional
    public TransformationResponse update(Long id, TransformationRequest request, UserEntity actor) {
        Transformation transformation = findVisible(id, actor);
        Permissions.requireOwnerOrAdmin(actor, transformation.getCreatedBy(), "la transformación");
        if (transformationRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new ConflictException("Ya existe una transformación con el nombre '" + request.name() + "'");
        }
        define(transformation, request);
        return TransformationResponse.from(transformationRepository.save(transformation));
    }

    @Transactional
    public TransformationResponse setPublished(Long id, boolean published, UserEntity actor) {
        if (!Permissions.isAdmin(actor)) {
            throw new ForbiddenOperationException("Solo un administrador puede publicar o retirar transformaciones");
        }
        Transformation transformation = findVisible(id, actor);
        transformation.setPublished(published);
        return TransformationResponse.from(transformationRepository.save(transformation));
    }

    @Transactional
    public void delete(Long id, UserEntity actor) {
        Transformation transformation = findVisible(id, actor);
        Permissions.requireOwnerOrAdmin(actor, transformation.getCreatedBy(), "la transformación");
        executionRepository.detachFromTransformation(transformation);
        transformationRepository.delete(transformation);
    }

    private void define(Transformation transformation, TransformationRequest request) {
        Metamodel input = metamodelService.find(request.inputMetamodelId());
        AtlModule atl = request.atlModuleId() == null ? null : atlModuleService.find(request.atlModuleId());
        AcceleoModule acceleo = request.acceleoModuleId() == null ? null
                : acceleoModuleService.find(request.acceleoModuleId());
        validateChain(input, atl, acceleo);
        transformation.define(request.name().trim(), request.description(), input, atl, acceleo);
    }

    /** Cada etapa debe consumir el metamodelo que produce la anterior. */
    private static void validateChain(Metamodel input, AtlModule atl, AcceleoModule acceleo) {
        if (atl == null && acceleo == null) {
            throw new IllegalArgumentException("Indique al menos un módulo: atlModuleId (modelo a modelo) y/o "
                    + "acceleoModuleId (modelo a código)");
        }
        if (atl != null && !atl.getSourceMetamodel().getId().equals(input.getId())) {
            throw new IllegalArgumentException("El módulo ATL '" + atl.getName() + "' recibe modelos de "
                    + atl.getSourceMetamodel().getName() + ", no de " + input.getName());
        }
        Metamodel codeInput = atl != null ? atl.getTargetMetamodel() : input;
        if (acceleo != null && !acceleo.getMetamodel().getId().equals(codeInput.getId())) {
            throw new IllegalArgumentException("El módulo Acceleo '" + acceleo.getName() + "' genera código a partir "
                    + "de " + acceleo.getMetamodel().getName() + ", pero recibiría un modelo de " + codeInput.getName());
        }
    }
}
