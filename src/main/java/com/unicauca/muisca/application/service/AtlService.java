package com.unicauca.muisca.application.service;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.m2m.atl.engine.vm.AtlLauncher;
//import org.eclipse.core.runtime.NullProgressMonitor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AtlService {

    public String executeTransformation(String inputModelPath, String inputMetamodelPath, 
                                        String outputMetamodelPath, String outputModelPath) {
        try {
            // Registrar la extensión XMI para EMF
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

            // Registrar metamodelos
            ResourceSet resourceSet = new ResourceSetImpl();
            EPackage inputMetamodel = loadMetamodel(resourceSet, inputMetamodelPath);
            EPackage outputMetamodel = loadMetamodel(resourceSet, outputMetamodelPath);

            // Registrar en el repositorio global de EPackage
            EPackage.Registry.INSTANCE.put(inputMetamodel.getNsURI(), inputMetamodel);
            EPackage.Registry.INSTANCE.put(outputMetamodel.getNsURI(), outputMetamodel);

            // Cargar modelos
            Resource inputModel = resourceSet.getResource(URI.createFileURI(inputModelPath), true);
            Resource outputModel = resourceSet.createResource(URI.createFileURI(outputModelPath));

            // Configuración y ejecución de ATL
            System.out.println("Ejecutando transformación ATL...");
            Map<String, Object> options = new HashMap<>();
            options.put("allowInterModelReferences", true);

            String transformationPath = new File("transformacion.asm").getAbsolutePath();
            AtlLauncher launcher = AtlLauncher.getDefault();
            List<Resource> outputModels = List.of(outputModel);
            launcher.launch(
                new File(transformationPath).toURI().toURL(), // Convierte transformationPath a URL
                new HashMap<>(), // libraries, si no tienes bibliotecas, pasa un mapa vacío
                Map.of("IN", inputModel), // models, mapea los modelos de entrada
                new HashMap<>(), // asmParams, si no tienes parámetros específicos, pasa un mapa vacío
                outputModels, // superimpose, lista de modelos de salida
                options, // options, mapa de opciones
                null // debugger, pasa null si no necesitas depuración
            );

            // Guardar modelo de salida
            outputModel.save(Map.of());
            return "Transformación completada exitosamente.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error durante la transformación: " + e.getMessage();
        }
    }

    private EPackage loadMetamodel(ResourceSet resourceSet, String metamodelPath) throws Exception {
        Resource metamodelResource = resourceSet.getResource(URI.createFileURI(metamodelPath), true);
        return (EPackage) metamodelResource.getContents().get(0);
    }
}