package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.MetadataRequest;
import com.unicauca.muisca.application.dto.MetamodelResponse;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.MetamodelInspector;
import com.unicauca.muisca.application.port.MetamodelInspector.EcoreInfo;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.AcceleoModuleRepository;
import com.unicauca.muisca.domain.repository.AtlModuleRepository;
import com.unicauca.muisca.domain.repository.MetamodelRepository;
import com.unicauca.muisca.domain.repository.StoredModelRepository;
import com.unicauca.muisca.domain.repository.TransformationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MetamodelService {

    private final MetamodelRepository metamodelRepository;
    private final AtlModuleRepository atlModuleRepository;
    private final AcceleoModuleRepository acceleoModuleRepository;
    private final TransformationRepository transformationRepository;
    private final StoredModelRepository storedModelRepository;
    private final MetamodelInspector inspector;

    public MetamodelService(MetamodelRepository metamodelRepository, AtlModuleRepository atlModuleRepository,
            AcceleoModuleRepository acceleoModuleRepository, TransformationRepository transformationRepository,
            StoredModelRepository storedModelRepository, MetamodelInspector inspector) {
        this.metamodelRepository = metamodelRepository;
        this.atlModuleRepository = atlModuleRepository;
        this.acceleoModuleRepository = acceleoModuleRepository;
        this.transformationRepository = transformationRepository;
        this.storedModelRepository = storedModelRepository;
        this.inspector = inspector;
    }

    @Transactional(readOnly = true)
    public List<MetamodelResponse> list() {
        return metamodelRepository.findAllByOrderByNameAsc().stream().map(MetamodelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MetamodelResponse get(Long id) {
        return MetamodelResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public Metamodel find(Long id) {
        return metamodelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el metamodelo con id " + id));
    }

    @Transactional
    public MetamodelResponse create(String name, String description, UploadedFile ecore, UploadedFile genmodel,
            UserEntity actor) {
        requireText(name, "name");
        if (metamodelRepository.existsByName(name)) {
            throw new ConflictException("Ya existe un metamodelo con el nombre '" + name + "'");
        }
        ecore.requireExtension(".ecore");
        EcoreInfo info = inspector.inspect(ecore.filename(), ecore.content());
        if (metamodelRepository.existsByNsUri(info.nsUri())) {
            throw new ConflictException("Ya existe un metamodelo registrado con el nsURI '" + info.nsUri() + "'");
        }
        byte[] genmodelData = null;
        if (genmodel != null) {
            genmodelData = genmodel.requireExtension(".genmodel").content();
        }
        Metamodel metamodel = new Metamodel(name.trim(), description, info.nsUri(), ecore.filename(),
                ecore.content(), genmodelData, actor);
        return MetamodelResponse.from(metamodelRepository.save(metamodel), info.warnings());
    }

    /** Cambia nombre y descripción; el archivo .ecore (y su nsURI) no se modifican. */
    @Transactional
    public MetamodelResponse updateMetadata(Long id, MetadataRequest request, UserEntity actor) {
        Metamodel metamodel = find(id);
        Permissions.requireOwnerOrAdmin(actor, metamodel.getCreatedBy(), "el metamodelo");
        String name = request.name().trim();
        if (metamodelRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Ya existe un metamodelo con el nombre '" + name + "'");
        }
        metamodel.describe(name, StoredModelService.blankToNull(request.description()));
        return MetamodelResponse.from(metamodelRepository.save(metamodel));
    }

    @Transactional
    public void delete(Long id, UserEntity actor) {
        Metamodel metamodel = find(id);
        Permissions.requireOwnerOrAdmin(actor, metamodel.getCreatedBy(), "el metamodelo");
        if (atlModuleRepository.existsBySourceMetamodelOrTargetMetamodel(metamodel, metamodel)
                || acceleoModuleRepository.existsByMetamodel(metamodel)
                || transformationRepository.existsByInputMetamodel(metamodel)) {
            throw new ConflictException("El metamodelo '" + metamodel.getName() + "' está en uso por módulos o "
                    + "transformaciones; elimínelos primero");
        }
        if (storedModelRepository.existsByMetamodel(metamodel)) {
            throw new ConflictException("El metamodelo '" + metamodel.getName() + "' tiene modelos guardados en "
                    + "bibliotecas de usuarios; deben eliminarse primero");
        }
        metamodelRepository.delete(metamodel);
    }

    static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El campo '" + field + "' es obligatorio");
        }
    }
}
