package com.unicauca.muisca.infraestructure.web;

import com.unicauca.muisca.application.service.AtlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/atl")
public class AtlController {

    // Directorio base donde se encuentran las transformaciones
    public final static String TRANSFORMATION_DIR = "atl/Transformations/ATL/";
    public final static String OUT_MODEL = "src/main/resources/atl/Models/SCJavaTest.xmi";

    @Autowired
    private AtlService atlService;

    @PostMapping("/transform")
    public ResponseEntity<?> transform(
            @RequestParam MultipartFile inModel,
            @RequestParam MultipartFile inMetamodel,
            @RequestParam MultipartFile outMetamodel,
            @RequestParam String transformationType) {
        File inMetamodelFile = null;
        File inModelFile = null;
        File outMetamodelFile = null;

        try {
            // Validar formatos de archivo
            validateFileFormat(inModel, ".xmi");
            validateFileFormat(inMetamodel, ".ecore");
            validateFileFormat(outMetamodel, ".ecore");

            // Guardar archivos en el directorio temporal
            inMetamodelFile = saveTempFile(inMetamodel);
            inModelFile = saveTempFile(inModel);
            outMetamodelFile = saveTempFile(outMetamodel);

            // Ruta para el modelo de salida
            // String outModelPath = System.getProperty("java.io.tmpdir") +
            // "/SCJavaTest.xmi";

            System.out.println("Ruta del modelo de entrada: " + inModelFile.getAbsolutePath());
            System.out.println("Ruta del metamodelo de entrada: " + inMetamodelFile.getAbsolutePath());
            System.out.println("Ruta del metamodelo de salida: " + outMetamodelFile.getAbsolutePath());


            // Ejecutar la transformación
            atlService.executeTransformation(
                    inMetamodelFile.getAbsolutePath(),
                    inModelFile.getAbsolutePath(),
                    outMetamodelFile.getAbsolutePath(),
                    OUT_MODEL,
                    TRANSFORMATION_DIR,
                    transformationType);

            // Devolver respuesta exitosa
            return ResponseEntity.ok("Transformación completada exitosamente. Modelo de salida: " + OUT_MODEL);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error durante la transformación: " + e.getMessage());
        } finally {
            // Eliminar archivos temporales
            if (inMetamodelFile != null && inMetamodelFile.exists())
                inMetamodelFile.delete();
            if (inModelFile != null && inModelFile.exists())
                inModelFile.delete();
            if (outMetamodelFile != null && outMetamodelFile.exists())
                outMetamodelFile.delete();
        }
    }

    /**
     * Valida que el archivo tenga la extensión esperada.
     *
     * @param file              Archivo subido por el usuario.
     * @param expectedExtension Extensión esperada (por ejemplo, ".xmi" o ".ecore").
     * @throws IllegalArgumentException Si el archivo no tiene la extensión
     *                                  esperada.
     */
    private void validateFileFormat(MultipartFile file, String expectedExtension) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(expectedExtension)) {
            throw new IllegalArgumentException(
                    "El archivo " + originalFilename + " debe tener la extensión " + expectedExtension);
        }
    }

    /**
     * Guarda un archivo subido en el directorio temporal del sistema.
     *
     * @param file Archivo subido por el usuario.
     * @return Archivo temporal guardado.
     * @throws IOException Si ocurre un error al guardar el archivo.
     */
    private File saveTempFile(MultipartFile file) throws IOException {
        // Extraer la extensión del archivo original
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Crear archivo temporal con la misma extensión
        File tempFile = File.createTempFile("temp", extension);
        file.transferTo(tempFile);
        return tempFile;
    }
}