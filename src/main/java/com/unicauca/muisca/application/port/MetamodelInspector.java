package com.unicauca.muisca.application.port;

import com.unicauca.muisca.domain.model.Metamodel;

import java.util.List;

/** Lectura y validación de metamodelos Ecore y de modelos conformes a ellos. */
public interface MetamodelInspector {

    /** @param warnings hallazgos del validador de EMF que no impiden usar el metamodelo */
    record EcoreInfo(String packageName, String nsUri, List<ArtifactProblem> warnings) {
    }

    /** @throws com.unicauca.muisca.application.exception.InvalidArtifactException si no es un Ecore válido */
    EcoreInfo inspect(String fileName, byte[] ecore);

    /** @throws com.unicauca.muisca.application.exception.InvalidArtifactException si el modelo no es conforme */
    void validateModel(Metamodel metamodel, String fileName, byte[] model);
}
