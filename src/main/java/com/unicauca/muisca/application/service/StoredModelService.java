package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.MetadataRequest;
import com.unicauca.muisca.application.dto.StoredModelResponse;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.MetamodelInspector;
import com.unicauca.muisca.domain.model.Execution;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.StoredModel;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.ExecutionRepository;
import com.unicauca.muisca.domain.repository.MetamodelRepository;
import com.unicauca.muisca.domain.repository.StoredModelRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Biblioteca de modelos: cada usuario guarda sus modelos .xmi para ejecutarlos varias veces o
 * encadenar transformaciones (por ejemplo, guardar el modelo Java producido por Go2Java y luego
 * generar su código). El metamodelo se detecta por el nsURI de la raíz del modelo.
 */
@Service
public class StoredModelService {

    private final StoredModelRepository storedModelRepository;
    private final MetamodelRepository metamodelRepository;
    private final ExecutionRepository executionRepository;
    private final MetamodelInspector inspector;

    public StoredModelService(StoredModelRepository storedModelRepository, MetamodelRepository metamodelRepository,
            ExecutionRepository executionRepository, MetamodelInspector inspector) {
        this.storedModelRepository = storedModelRepository;
        this.metamodelRepository = metamodelRepository;
        this.executionRepository = executionRepository;
        this.inspector = inspector;
    }

    /** Cada usuario ve sus modelos; el administrador, los de todos. */
    @Transactional(readOnly = true)
    public List<StoredModelResponse> list(UserEntity actor) {
        List<StoredModel> models = Permissions.isAdmin(actor) ? storedModelRepository.findAllByOrderByNameAsc()
                : storedModelRepository.findByOwnerOrderByNameAsc(actor);
        return models.stream().map(StoredModelResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public StoredModelResponse get(Long id, UserEntity actor) {
        return StoredModelResponse.from(find(id, actor));
    }

    /** Un modelo ajeno "no existe" para quien no es su dueño ni administrador. */
    @Transactional(readOnly = true)
    public StoredModel find(Long id, UserEntity actor) {
        return storedModelRepository.findById(id)
                .filter(m -> Permissions.isAdmin(actor) || m.getOwner().getId().equals(actor.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No existe el modelo con id " + id));
    }

    /** El contenido del modelo guardado como archivo, para ejecutarlo. */
    @Transactional(readOnly = true)
    public UploadedFile asUploadedFile(Long id, UserEntity actor) {
        StoredModel model = find(id, actor);
        return new UploadedFile(model.getFileName(), model.getContent());
    }

    /** @param name opcional; si falta se usa el nombre del archivo sin extensión */
    @Transactional
    public StoredModelResponse create(String name, String description, UploadedFile file, UserEntity actor) {
        file.requireExtension(".xmi", ".xml");
        Metamodel metamodel = detectMetamodel(file);
        inspector.validateModel(metamodel, file.filename(), file.content());
        String finalName = name == null || name.isBlank() ? ExecutionService.baseName(file.filename()) : name.trim();
        if (storedModelRepository.existsByOwnerAndName(actor, finalName)) {
            throw new ConflictException("Ya tiene un modelo llamado '" + finalName + "'");
        }
        StoredModel model = new StoredModel(finalName, blankToNull(description), metamodel, file.filename(),
                file.content(), actor);
        return StoredModelResponse.from(storedModelRepository.save(model));
    }

    /** Guarda en la biblioteca el modelo transformado que produjo una ejecución propia. */
    @Transactional
    public StoredModelResponse createFromExecution(Long executionId, String name, UserEntity actor) {
        Execution execution = executionRepository.findById(executionId)
                .filter(e -> Permissions.isAdmin(actor) || e.getExecutedBy().getId().equals(actor.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la ejecución con id " + executionId));
        if (execution.getOutputModel() == null) {
            throw new ConflictException("La ejecución " + executionId + " no produjo un modelo transformado");
        }
        return create(name, "Resultado de la ejecución #" + executionId + " (" + execution.getTransformationName()
                + ")", new UploadedFile(execution.getOutputModelName(), execution.getOutputModel()), actor);
    }

    @Transactional
    public StoredModelResponse updateMetadata(Long id, MetadataRequest request, UserEntity actor) {
        StoredModel model = find(id, actor);
        String name = request.name().trim();
        if (storedModelRepository.existsByOwnerAndNameAndIdNot(model.getOwner(), name, id)) {
            throw new ConflictException("Ya existe un modelo llamado '" + name + "'");
        }
        model.describe(name, blankToNull(request.description()));
        return StoredModelResponse.from(storedModelRepository.save(model));
    }

    @Transactional
    public void delete(Long id, UserEntity actor) {
        storedModelRepository.delete(find(id, actor));
    }

    private Metamodel detectMetamodel(UploadedFile file) {
        String nsUri = inspector.modelNsUri(file.filename(), file.content());
        return metamodelRepository.findByNsUri(nsUri).orElseThrow(() -> new InvalidArtifactException(
                "El modelo '" + file.filename() + "' pertenece al metamodelo '" + nsUri
                        + "', que no está registrado en MUISCA"));
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
