package com.unicauca.muisca.application.service;

import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.port.CodeGenerationEngine;
import com.unicauca.muisca.application.port.CodeGenerationEngine.TemplateCompilation;
import com.unicauca.muisca.application.port.MetamodelInspector;
import com.unicauca.muisca.application.port.MetamodelInspector.EcoreInfo;
import com.unicauca.muisca.application.port.ModelTransformationEngine;
import com.unicauca.muisca.application.port.ModelTransformationEngine.AtlCompilation;
import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.AtlModule;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.UserEntity;

import org.springframework.stereotype.Service;

/**
 * Espacio de trabajo del ingeniero de modelos, equivalente a una configuración de ejecución
 * de Eclipse: sube metamodelos, módulo y modelo de entrada, y obtiene el resultado sin
 * registrar nada en el catálogo.
 */
@Service
public class WorkspaceService {

    private final MetamodelInspector inspector;
    private final ModelTransformationEngine transformationEngine;
    private final CodeGenerationEngine codeGenerationEngine;

    public WorkspaceService(MetamodelInspector inspector, ModelTransformationEngine transformationEngine,
            CodeGenerationEngine codeGenerationEngine) {
        this.inspector = inspector;
        this.transformationEngine = transformationEngine;
        this.codeGenerationEngine = codeGenerationEngine;
    }

    public AtlCompilation compileAtl(UploadedFile atlFile) {
        return transformationEngine.compile(atlFile.requireExtension(".atl").text());
    }

    /** @return modelo de salida en XMI */
    public byte[] runAtl(UploadedFile sourceEcore, UploadedFile targetEcore, UploadedFile atlFile,
            UploadedFile inputModel, UserEntity actor) {
        Metamodel source = transientMetamodel(sourceEcore, actor);
        Metamodel target = transientMetamodel(targetEcore, actor);
        AtlCompilation compilation = compileAtl(atlFile);
        if (compilation.hasErrors()) {
            throw new InvalidArtifactException("El módulo ATL '" + atlFile.filename() + "' no compila",
                    compilation.problems());
        }
        AtlModule module = new AtlModule(compilation.moduleName(), null, source, target, actor);
        module.applyCompilation(atlFile.text(), compilation.compiler(), compilation.moduleName(),
                compilation.inModelName(), compilation.sourceAlias(), compilation.outModelName(),
                compilation.targetAlias(), compilation.compiledModule());
        validateModel(source, inputModel);
        return transformationEngine.transform(module, inputModel.content());
    }

    public TemplateCompilation compileTemplate(UploadedFile ecore, UploadedFile templateFile, UserEntity actor) {
        return codeGenerationEngine.compile(templateFile.requireExtension(".mtl").text(),
                transientMetamodel(ecore, actor));
    }

    /** @return ZIP con los archivos generados */
    public byte[] runTemplate(UploadedFile ecore, UploadedFile templateFile, UploadedFile inputModel,
            UserEntity actor) {
        Metamodel metamodel = transientMetamodel(ecore, actor);
        TemplateCompilation compilation = codeGenerationEngine.compile(
                templateFile.requireExtension(".mtl").text(), metamodel);
        if (compilation.hasErrors()) {
            throw new InvalidArtifactException("La plantilla Acceleo '" + templateFile.filename() + "' no compila",
                    compilation.problems());
        }
        AcceleoModule module = new AcceleoModule(compilation.moduleName(), null, metamodel, actor);
        module.applyCompilation(templateFile.text(), compilation.moduleName(), compilation.mainTemplate());
        validateModel(metamodel, inputModel);
        return CodeArchive.zip(codeGenerationEngine.generate(module, inputModel.content()));
    }

    private Metamodel transientMetamodel(UploadedFile ecore, UserEntity actor) {
        ecore.requireExtension(".ecore");
        EcoreInfo info = inspector.inspect(ecore.filename(), ecore.content());
        return new Metamodel(info.packageName(), null, info.nsUri(), ecore.filename(), ecore.content(), null, actor);
    }

    private void validateModel(Metamodel metamodel, UploadedFile inputModel) {
        inputModel.requireExtension(".xmi", ".xml");
        inspector.validateModel(metamodel, inputModel.filename(), inputModel.content());
    }
}
