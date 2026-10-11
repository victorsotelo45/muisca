package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.MetadataRequest;
import com.unicauca.muisca.application.dto.AtlModuleResponse;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.application.port.ModelTransformationEngine;
import com.unicauca.muisca.application.port.ModelTransformationEngine.AtlCompilation;
import com.unicauca.muisca.domain.model.AtlModule;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.AtlModuleRepository;
import com.unicauca.muisca.domain.repository.TransformationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AtlModuleService {

    private final AtlModuleRepository atlModuleRepository;
    private final TransformationRepository transformationRepository;
    private final MetamodelService metamodelService;
    private final ModelTransformationEngine engine;

    public AtlModuleService(AtlModuleRepository atlModuleRepository, TransformationRepository transformationRepository,
            MetamodelService metamodelService, ModelTransformationEngine engine) {
        this.atlModuleRepository = atlModuleRepository;
        this.transformationRepository = transformationRepository;
        this.metamodelService = metamodelService;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public List<AtlModuleResponse> list() {
        return atlModuleRepository.findAllByOrderByNameAsc().stream().map(AtlModuleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AtlModuleResponse get(Long id) {
        return AtlModuleResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public AtlModule find(Long id) {
        return atlModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el módulo ATL con id " + id));
    }

    @Transactional
    public AtlModuleResponse create(String name, String description, Long sourceMetamodelId, Long targetMetamodelId,
            UploadedFile atlFile, UserEntity actor) {
        MetamodelService.requireText(name, "name");
        if (atlModuleRepository.existsByName(name)) {
            throw new ConflictException("Ya existe un módulo ATL con el nombre '" + name + "'");
        }
        Metamodel source = metamodelService.find(sourceMetamodelId);
        Metamodel target = metamodelService.find(targetMetamodelId);
        AtlModule module = new AtlModule(name.trim(), description, source, target, actor);
        List<ArtifactProblem> warnings = compileInto(module, atlFile);
        return AtlModuleResponse.from(atlModuleRepository.save(module), warnings);
    }

    /** Reemplaza el código fuente (por ejemplo, tras corregirlo en el espacio de trabajo). */
    @Transactional
    public AtlModuleResponse updateSource(Long id, UploadedFile atlFile, UserEntity actor) {
        AtlModule module = find(id);
        Permissions.requireOwnerOrAdmin(actor, module.getCreatedBy(), "el módulo ATL");
        List<ArtifactProblem> warnings = compileInto(module, atlFile);
        return AtlModuleResponse.from(atlModuleRepository.save(module), warnings);
    }

    /** Cambia nombre y descripción sin recompilar el código. */
    @Transactional
    public AtlModuleResponse updateMetadata(Long id, MetadataRequest request, UserEntity actor) {
        AtlModule module = find(id);
        Permissions.requireOwnerOrAdmin(actor, module.getCreatedBy(), "el módulo ATL");
        String name = request.name().trim();
        if (atlModuleRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Ya existe un módulo ATL con el nombre '" + name + "'");
        }
        module.describe(name, StoredModelService.blankToNull(request.description()));
        return AtlModuleResponse.from(atlModuleRepository.save(module));
    }

    @Transactional
    public void delete(Long id, UserEntity actor) {
        AtlModule module = find(id);
        Permissions.requireOwnerOrAdmin(actor, module.getCreatedBy(), "el módulo ATL");
        if (transformationRepository.existsByAtlModule(module)) {
            throw new ConflictException("El módulo ATL '" + module.getName() + "' está en uso por una "
                    + "transformación del catálogo; elimínela primero");
        }
        atlModuleRepository.delete(module);
    }

    private List<ArtifactProblem> compileInto(AtlModule module, UploadedFile atlFile) {
        String source = atlFile.requireExtension(".atl").text();
        AtlCompilation compilation = engine.compile(source);
        if (compilation.hasErrors()) {
            throw new InvalidArtifactException("El módulo ATL '" + atlFile.filename() + "' no compila",
                    compilation.problems());
        }
        module.applyCompilation(source, compilation.compiler(), compilation.moduleName(), compilation.inModelName(),
                compilation.sourceAlias(), compilation.outModelName(), compilation.targetAlias(),
                compilation.compiledModule());
        return compilation.problems();
    }
}
