package com.unicauca.muisca.application.service;

import org.eclipse.emf.common.util.URI;
import org.eclipse.m2m.atl.core.ATLCoreException;
import org.eclipse.m2m.atl.core.IInjector;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.emf.EMFInjector;
import org.eclipse.m2m.atl.core.emf.EMFModel;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.engine.emfvm.launch.EMFVMLauncher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class AtlService {

    public String executeTransformation(String transformationAsmPath,
                                        MultipartFile inputModelFile,
                                        MultipartFile inputMetamodelFile,
                                        MultipartFile outputMetamodelFile,
                                        String outputModelPath,
                                        String inputModelName,
                                        String outputModelName) {
        try {
            // Crear archivos temporales
            File inputModel = File.createTempFile("inputModel_", ".xmi");
            inputModelFile.transferTo(inputModel);

            File inputMetamodel = File.createTempFile("inputMetamodel_", ".ecore");
            inputMetamodelFile.transferTo(inputMetamodel);

            File outputMetamodel = File.createTempFile("outputMetamodel_", ".ecore");
            outputMetamodelFile.transferTo(outputMetamodel);

            File outputModel = new File(outputModelPath);
            outputModel.getParentFile().mkdirs();

            // Inicializar recursos EMF
            EMFModelFactory modelFactory = new EMFModelFactory();
            IInjector injector = new EMFInjector();

            // Cargar metamodelo de entrada
            IReferenceModel inMetaModel = modelFactory.newReferenceModel();
            injector.inject(inMetaModel, "file:///" + inputMetamodel.getAbsolutePath().replace("\\", "/"));

            // Cargar metamodelo de salida
            IReferenceModel outMetaModel = modelFactory.newReferenceModel();
            injector.inject(outMetaModel, "file:///" + outputMetamodel.getAbsolutePath().replace("\\", "/"));

            // Crear e inyectar modelo de entrada
            IModel inModel = modelFactory.newModel(inMetaModel);
            injector.inject(inModel, "file:///" + inputModel.getAbsolutePath().replace("\\", "/"));

            // Crear modelo de salida vacío
            IModel outModel = modelFactory.newModel(outMetaModel);

            // Ejecutar transformación
            EMFVMLauncher launcher = new EMFVMLauncher();
            launcher.initialize(Collections.emptyMap());

            launcher.addInModel(inModel, inputModelName, "IN");
            launcher.addOutModel(outModel, outputModelName, "OUT");

            Map<String, Object> options = new HashMap<>();
            options.put("allowInterModelReferences", true);

            try (FileInputStream asmInputStream = new FileInputStream(transformationAsmPath)) {
                launcher.launch("run", null, options, asmInputStream);
            }

            // Guardar el modelo de salida
            if (outModel instanceof EMFModel emfOutModel) {
                emfOutModel.getResource().setURI(
                        URI.createFileURI(outputModel.getAbsolutePath())
                );
                emfOutModel.getResource().save(Collections.emptyMap());
                return "✅ Transformación completada exitosamente.";
            } else {
                return "❌ Error: El modelo de salida no es una instancia de EMFModel.";
            }

        } catch (ATLCoreException e) {
            e.printStackTrace();
            return "❌ Error ATL: " + e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error general: " + e.getMessage();
        }
    }
}