package com.unicauca.muisca.infrastructure.web;

import com.unicauca.muisca.application.dto.Reference;
import com.unicauca.muisca.application.dto.SampleModelResponse;
import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.exception.ResourceNotFoundException;
import com.unicauca.muisca.application.port.MetamodelInspector;
import com.unicauca.muisca.domain.repository.MetamodelRepository;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Modelos de ejemplo (src/main/resources/mde/models) para probar una transformación sin tener un
 * modelo propio. Solo se listan los de metamodelos registrados en el catálogo.
 */
@RestController
@RequestMapping("/api/samples")
public class SampleModelController {

    private static final String LOCATION = "classpath:mde/models/*.xmi";

    private final MetamodelInspector inspector;
    private final MetamodelRepository metamodelRepository;

    public SampleModelController(MetamodelInspector inspector, MetamodelRepository metamodelRepository) {
        this.inspector = inspector;
        this.metamodelRepository = metamodelRepository;
    }

    @GetMapping
    public List<SampleModelResponse> list() {
        List<SampleModelResponse> samples = new ArrayList<>();
        for (Resource resource : resources()) {
            byte[] content = read(resource);
            nsUriOf(resource.getFilename(), content)
                    .flatMap(metamodelRepository::findByNsUri)
                    .ifPresent(m -> samples.add(new SampleModelResponse(resource.getFilename(),
                            new Reference(m.getId(), m.getName()), content.length)));
        }
        samples.sort(Comparator.comparing(SampleModelResponse::fileName));
        return samples;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<byte[]> download(@PathVariable String fileName) {
        // Solo nombres de la carpeta de ejemplos: se compara con el listado, nunca se arma una ruta.
        Resource resource = resources().stream()
                .filter(r -> fileName.equals(r.getFilename()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No existe el modelo de ejemplo " + fileName));
        return Files.download(resource.getFilename(), read(resource), Files.XML);
    }

    private Optional<String> nsUriOf(String fileName, byte[] content) {
        try {
            return Optional.of(inspector.modelNsUri(fileName, content));
        } catch (InvalidArtifactException e) {
            return Optional.empty();
        }
    }

    private static List<Resource> resources() {
        try {
            return List.of(new PathMatchingResourcePatternResolver().getResources(LOCATION));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] read(Resource resource) {
        try {
            return resource.getContentAsByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
