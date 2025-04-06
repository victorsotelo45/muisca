package com.unicauca.muisca.application.service;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.engine.vm.AtlLauncher;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AtlService {

    public String executeTransformation(String transformationAsmPath,
                                        String inputModelPath,
                                        String inputMetamodelPath,
                                        String outputMetamodelPath,
                                        String outputModelPath,
                                        String inputModelName,
                                        String outputModelName) {
        try {
            // ✅ Registrar fábricas para .xmi y .ecore
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

            // Crear el ResourceSet
            ResourceSet resourceSet = new ResourceSetImpl();

            // Cargar metamodelos
            EPackage inputMetamodel = loadMetamodel(resourceSet, inputMetamodelPath);
            EPackage outputMetamodel = loadMetamodel(resourceSet, outputMetamodelPath);

            // Registrar metamodelos globalmente
            EPackage.Registry.INSTANCE.put(inputMetamodel.getNsURI(), inputMetamodel);
            EPackage.Registry.INSTANCE.put(outputMetamodel.getNsURI(), outputMetamodel);

            // Cargar modelo de entrada y preparar modelo de salida
            Resource inputModel = resourceSet.getResource(URI.createFileURI(inputModelPath), true);
            Resource outputModel = resourceSet.createResource(URI.createFileURI(outputModelPath));

            // Configurar ejecución ATL
            Map<String, Object> options = new HashMap<>();
            options.put("allowInterModelReferences", true);

            AtlLauncher launcher = AtlLauncher.getDefault();

            launcher.launch(
                    new File(transformationAsmPath).toURI().toURL(),
                    new HashMap<>(),
                    Map.of(
                            inputModelName, inputModel,
                            outputModelName, outputModel
                    ),
                    new HashMap<>(),
                    List.of(),
                    options,
                    null
            );

            // Guardar el modelo de salida
            outputModel.save(Map.of());

            return "✅ Transformación completada exitosamente.";
        } catch (Exception e) {
            e.printStackTrace();
            return "❌ Error durante la transformación: " + e.getMessage();
        }
    }

    private EPackage loadMetamodel(ResourceSet resourceSet, String metamodelPath) throws Exception {
        Resource metamodelResource = resourceSet.getResource(URI.createFileURI(metamodelPath), true);
        return (EPackage) metamodelResource.getContents().get(0);
    }
}
