package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.MetadataRequest;
import com.unicauca.muisca.application.dto.AcceleoModuleResponse;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.application.port.CodeGenerationEngine;
import com.unicauca.muisca.application.port.CodeGenerationEngine.TemplateCompilation;
import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.AcceleoModuleRepository;
import com.unicauca.muisca.domain.repository.TransformationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcceleoModuleService {

    private final AcceleoModuleRepository acceleoModuleRepository;
    private final TransformationRepository transformationRepository;
    private final MetamodelService metamodelService;
    private final CodeGenerationEngine engine;

    public AcceleoModuleService(AcceleoModuleRepository acceleoModuleRepository,
            TransformationRepository transformationRepository, MetamodelService metamodelService,
            CodeGenerationEngine engine) {
        this.acceleoModuleRepository = acceleoModuleRepository;
        this.transformationRepository = transformationRepository;
        this.metamodelService = metamodelService;
        this.engine = engine;
    }

    @Transactional(readOnly = true)
    public List<AcceleoModuleResponse> list() {
        return acceleoModuleRepository.findAllByOrderByNameAsc().stream().map(AcceleoModuleResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AcceleoModuleResponse get(Long id) {
        return AcceleoModuleResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public AcceleoModule find(Long id) {
        return acceleoModuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el módulo Acceleo con id " + id));
    }

    @Transactional
    public AcceleoModuleResponse create(String name, String description, Long metamodelId, UploadedFile templateFile,
            UserEntity actor) {
        MetamodelService.requireText(name, "name");
        if (acceleoModuleRepository.existsByName(name)) {
            throw new ConflictException("Ya existe un módulo Acceleo con el nombre '" + name + "'");
        }
        Metamodel metamodel = metamodelService.find(metamodelId);
        AcceleoModule module = new AcceleoModule(name.trim(), description, metamodel, actor);
        List<ArtifactProblem> warnings = compileInto(module, templateFile);
        return AcceleoModuleResponse.from(acceleoModuleRepository.save(module), warnings);
    }

    @Transactional
    public AcceleoModuleResponse updateSource(Long id, UploadedFile templateFile, UserEntity actor) {
        AcceleoModule module = find(id);
        Permissions.requireOwnerOrAdmin(actor, module.getCreatedBy(), "el módulo Acceleo");
        List<ArtifactProblem> warnings = compileInto(module, templateFile);
        return AcceleoModuleResponse.from(acceleoModuleRepository.save(module), warnings);
    }

    /** Cambia nombre y descripción sin recompilar el código. */
    @Transactional
    public AcceleoModuleResponse updateMetadata(Long id, MetadataRequest request, UserEntity actor) {
        AcceleoModule module = find(id);
        Permissions.requireOwnerOrAdmin(actor, module.getCreatedBy(), "el módulo Acceleo");
        String name = request.name().trim();
        if (acceleoModuleRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("Ya existe un módulo Acceleo con el nombre '" + name + "'");
        }
        module.describe(name, StoredModelService.blankToNull(request.description()));
        return AcceleoModuleResponse.from(acceleoModuleRepository.save(module));
    }

    @Transactional
    public void delete(Long id, UserEntity actor) {
        AcceleoModule module = find(id);
        Permissions.requireOwnerOrAdmin(actor, module.getCreatedBy(), "el módulo Acceleo");
        if (transformationRepository.existsByAcceleoModule(module)) {
            throw new ConflictException("El módulo Acceleo '" + module.getName() + "' está en uso por una "
                    + "transformación del catálogo; elimínela primero");
        }
        acceleoModuleRepository.delete(module);
    }

    private List<ArtifactProblem> compileInto(AcceleoModule module, UploadedFile templateFile) {
        String source = templateFile.requireExtension(".mtl").text();
        TemplateCompilation compilation = engine.compile(source, module.getMetamodel());
        if (compilation.hasErrors()) {
            throw new InvalidArtifactException("La plantilla Acceleo '" + templateFile.filename() + "' no compila",
                    compilation.problems());
        }
        module.applyCompilation(source, compilation.moduleName(), compilation.mainTemplate());
        return compilation.problems();
    }
}
