package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.ExecutionResponse;
import com.unicauca.muisca.application.dto.GeneratedFile;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.CodeGenerationEngine;
import com.unicauca.muisca.application.port.MetamodelInspector;
import com.unicauca.muisca.application.port.ModelTransformationEngine;
import com.unicauca.muisca.application.port.ModelingException;
import com.unicauca.muisca.domain.model.Execution;
import com.unicauca.muisca.domain.model.Transformation;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.ExecutionRepository;
import com.unicauca.muisca.domain.repository.ExecutionSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Ejecuta transformaciones del catálogo sobre el modelo del usuario y guarda el historial. */
@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;
    private final TransformationService transformationService;
    private final MetamodelInspector inspector;
    private final ModelTransformationEngine transformationEngine;
    private final CodeGenerationEngine codeGenerationEngine;

    public ExecutionService(ExecutionRepository executionRepository, TransformationService transformationService,
            MetamodelInspector inspector, ModelTransformationEngine transformationEngine,
            CodeGenerationEngine codeGenerationEngine) {
        this.executionRepository = executionRepository;
        this.transformationService = transformationService;
        this.inspector = inspector;
        this.transformationEngine = transformationEngine;
        this.codeGenerationEngine = codeGenerationEngine;
    }

    /**
     * Sin transacción a propósito: la transformación puede tardar y no debe retener una
     * conexión a la base de datos mientras corre.
     *
     * @throws com.unicauca.muisca.application.exception.InvalidArtifactException si el modelo
     *         no es conforme al metamodelo de entrada (no se registra ejecución)
     */
    public ExecutionResponse execute(Long transformationId, UploadedFile inputModel, UserEntity actor) {
        Transformation transformation = transformationService.findVisible(transformationId, actor);
        inputModel.requireExtension(".xmi", ".xml");
        inspector.validateModel(transformation.getInputMetamodel(), inputModel.filename(), inputModel.content());

        Execution execution = new Execution(transformation, actor, inputModel.filename(), inputModel.content());
        long start = System.currentTimeMillis();
        try {
            byte[] outputModel = null;
            String outputModelName = null;
            byte[] codeInput = inputModel.content();
            if (transformation.getAtlModule() != null) {
                outputModel = transformationEngine.transform(transformation.getAtlModule(), inputModel.content());
                outputModelName = baseName(inputModel.filename()) + "-" + transformation.getAtlModule().getModuleName()
                        + ".xmi";
                codeInput = outputModel;
            }
            List<String> fileNames = null;
            byte[] code = null;
            if (transformation.getAcceleoModule() != null) {
                Map<String, byte[]> files = codeGenerationEngine.generate(transformation.getAcceleoModule(), codeInput);
                fileNames = new ArrayList<>(files.keySet());
                code = CodeArchive.zip(files);
            }
            execution.succeed(outputModelName, outputModel, fileNames, code, System.currentTimeMillis() - start);
        } catch (ModelingException e) {
            log.warn("Falló la ejecución de '{}' por {}: {}", transformation.getName(), actor.getUsername(),
                    e.getMessage());
            execution.fail(e.getMessage(), System.currentTimeMillis() - start);
        }
        return ExecutionResponse.from(executionRepository.save(execution));
    }

    @Transactional(readOnly = true)
    public List<ExecutionSummary> list(UserEntity actor) {
        return Permissions.isAdmin(actor) ? executionRepository.findAllSummaries()
                : executionRepository.findSummariesByUser(actor);
    }

    @Transactional(readOnly = true)
    public ExecutionResponse get(Long id, UserEntity actor) {
        return ExecutionResponse.from(find(id, actor));
    }

    /** Un usuario solo accede a sus propias ejecuciones; el administrador, a todas. */
    @Transactional(readOnly = true)
    public Execution find(Long id, UserEntity actor) {
        return executionRepository.findById(id)
                .filter(e -> Permissions.isAdmin(actor) || e.getExecutedBy().getId().equals(actor.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la ejecución con id " + id));
    }

    @Transactional(readOnly = true)
    public List<GeneratedFile> generatedFiles(Long id, UserEntity actor) {
        Execution execution = find(id, actor);
        if (execution.getGeneratedCode() == null) {
            throw new ResourceNotFoundException("La ejecución " + id + " no generó código");
        }
        return CodeArchive.unzip(execution.getGeneratedCode()).entrySet().stream()
                .map(f -> new GeneratedFile(f.getKey(), new String(f.getValue(), StandardCharsets.UTF_8)))
                .toList();
    }

    @Transactional
    public void delete(Long id, UserEntity actor) {
        executionRepository.delete(find(id, actor));
    }

    static String baseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
