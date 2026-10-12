package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.ExecutionResponse;
import com.unicauca.muisca.application.dto.GeneratedFile;
import com.unicauca.muisca.application.dto.PageResponse;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.exception.ConflictException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.CodeGenerationEngine;
import com.unicauca.muisca.application.port.MetamodelInspector;
import com.unicauca.muisca.application.port.ModelTransformationEngine;
import com.unicauca.muisca.application.port.ModelingException;
import com.unicauca.muisca.domain.model.Execution;
import com.unicauca.muisca.domain.model.ExecutionStage;
import com.unicauca.muisca.domain.model.ExecutionStatus;
import com.unicauca.muisca.domain.model.Transformation;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.ExecutionRepository;
import com.unicauca.muisca.domain.repository.ExecutionSearchRepository;
import com.unicauca.muisca.domain.repository.ExecutionSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ejecuta transformaciones del catálogo sobre el modelo del usuario y guarda el historial. La
 * validación del modelo siempre es inmediata; el procesamiento puede ser síncrono (respuesta con
 * el resultado) o en segundo plano (respuesta RUNNING que el cliente consulta hasta que termine).
 */
@Service
public class ExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final ExecutionRepository executionRepository;
    private final TransformationService transformationService;
    private final MetamodelInspector inspector;
    private final ModelTransformationEngine transformationEngine;
    private final CodeGenerationEngine codeGenerationEngine;
    private final TaskExecutor executor;

    public ExecutionService(ExecutionRepository executionRepository, TransformationService transformationService,
            MetamodelInspector inspector, ModelTransformationEngine transformationEngine,
            CodeGenerationEngine codeGenerationEngine, @Qualifier("executionExecutor") TaskExecutor executor) {
        this.executionRepository = executionRepository;
        this.transformationService = transformationService;
        this.inspector = inspector;
        this.transformationEngine = transformationEngine;
        this.codeGenerationEngine = codeGenerationEngine;
        this.executor = executor;
    }

    /**
     * Ejecuta y espera el resultado. Sin transacción a propósito: la transformación puede tardar
     * y no debe retener una conexión a la base de datos mientras corre.
     *
     * @throws com.unicauca.muisca.application.exception.InvalidArtifactException si el modelo
     *         no es conforme al metamodelo de entrada (no se registra ejecución)
     */
    public ExecutionResponse execute(Long transformationId, UploadedFile inputModel, UserEntity actor) {
        Execution execution = executionRepository.save(prepare(transformationId, inputModel, actor));
        return ExecutionResponse.from(process(execution));
    }

    /** Valida, registra la ejecución como RUNNING y la procesa en segundo plano. */
    public ExecutionResponse start(Long transformationId, UploadedFile inputModel, UserEntity actor) {
        Execution execution = executionRepository.save(prepare(transformationId, inputModel, actor));
        try {
            executor.execute(() -> process(execution));
        } catch (TaskRejectedException e) {
            executionRepository.delete(execution);
            throw new ConflictException("Hay demasiadas ejecuciones en cola; intente de nuevo en unos minutos");
        }
        return ExecutionResponse.from(execution);
    }

    @Transactional(readOnly = true)
    public List<ExecutionSummary> list(UserEntity actor) {
        return Permissions.isAdmin(actor) ? executionRepository.findAllSummaries()
                : executionRepository.findSummariesByUser(actor);
    }

    /** Historial paginado y filtrado en el servidor. */
    @Transactional(readOnly = true)
    public PageResponse<ExecutionSummary> search(UserEntity actor, ExecutionStatus status, String text, int page,
            int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        ExecutionSearchRepository.Page result = executionRepository.search(
                Permissions.isAdmin(actor) ? null : actor, status, text, safePage, safeSize);
        return PageResponse.of(result.items(), safePage, safeSize, result.total());
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
        Execution execution = find(id, actor);
        if (execution.getStatus() == ExecutionStatus.RUNNING) {
            throw new ConflictException("La ejecución " + id + " todavía está en curso; espere a que termine");
        }
        executionRepository.delete(execution);
    }

    private Execution prepare(Long transformationId, UploadedFile inputModel, UserEntity actor) {
        Transformation transformation = transformationService.findVisible(transformationId, actor);
        inputModel.requireExtension(".xmi", ".xml");
        inspector.validateModel(transformation.getInputMetamodel(), inputModel.filename(), inputModel.content());
        return new Execution(transformation, actor, inputModel.filename(), inputModel.content());
    }

    /** Corre las etapas ATL y Acceleo; cualquier fallo queda registrado como FAILED. */
    private Execution process(Execution execution) {
        Transformation transformation = execution.getTransformation();
        long start = System.currentTimeMillis();
        try {
            byte[] outputModel = null;
            String outputModelName = null;
            byte[] codeInput = execution.getInputModel();
            if (transformation.getAtlModule() != null) {
                executionRepository.updateStage(execution.getId(), ExecutionStage.ATL);
                outputModel = transformationEngine.transform(transformation.getAtlModule(), execution.getInputModel());
                outputModelName = baseName(execution.getInputModelName()) + "-"
                        + transformation.getAtlModule().getModuleName() + ".xmi";
                codeInput = outputModel;
            }
            List<String> fileNames = null;
            byte[] code = null;
            if (transformation.getAcceleoModule() != null) {
                executionRepository.updateStage(execution.getId(), ExecutionStage.ACCELEO);
                Map<String, byte[]> files = codeGenerationEngine.generate(transformation.getAcceleoModule(), codeInput);
                fileNames = new ArrayList<>(files.keySet());
                code = CodeArchive.zip(files);
            }
            execution.succeed(outputModelName, outputModel, fileNames, code, System.currentTimeMillis() - start);
        } catch (ModelingException e) {
            log.warn("Falló la ejecución de '{}' por {}: {}", transformation.getName(),
                    execution.getExecutedBy().getUsername(), e.getMessage());
            execution.fail(e.getMessage(), System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            log.error("Error inesperado en la ejecución {}", execution.getId(), e);
            execution.fail("Error inesperado del motor de transformación. Intente de nuevo o contacte al "
                    + "administrador.", System.currentTimeMillis() - start);
        }
        try {
            return executionRepository.save(execution);
        } catch (RuntimeException e) {
            // Por ejemplo, si la transformación se eliminó mientras corría.
            log.error("No se pudo guardar el resultado de la ejecución {}", execution.getId(), e);
            return execution;
        }
    }

    static String baseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
