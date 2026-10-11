package com.unicauca.muisca.infrastructure.mde;

import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.port.ModelingException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/** Utilidades EMF compartidas por los motores ATL y Acceleo. */
final class EmfSupport {

    private static final String DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private EmfSupport() {
    }

    static ResourceSet newResourceSet() {
        ResourceSet rs = new ResourceSetImpl();
        Map<String, Object> factories = rs.getResourceFactoryRegistry().getExtensionToFactoryMap();
        factories.put("ecore", new EcoreResourceFactoryImpl());
        factories.put("xmi", new XMIResourceFactoryImpl());
        factories.put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        rs.getLoadOptions().put(XMLResource.OPTION_PARSER_FEATURES, Map.of(DISALLOW_DOCTYPE, true));
        return rs;
    }

    /**
     * Comprueba que el archivo sea XML bien formado y sin DOCTYPE (evita ataques XXE en los
     * modelos que suben los usuarios, que luego cargan EMF, ATL y Acceleo).
     */
    static void requireSafeXml(String fileName, byte[] content) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(DISALLOW_DOCTYPE, true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.newSAXParser().parse(new ByteArrayInputStream(content), new DefaultHandler());
        } catch (SAXParseException e) {
            throw new InvalidArtifactException("El archivo '" + fileName + "' no es un XML válido (línea "
                    + e.getLineNumber() + "): " + e.getMessage());
        } catch (Exception e) {
            throw new InvalidArtifactException("El archivo '" + fileName + "' no es un XML válido: " + e.getMessage());
        }
    }

    static Resource load(ResourceSet rs, String fileName, byte[] content) throws IOException {
        Resource resource = rs.createResource(URI.createURI("muisca:/" + fileName));
        resource.load(new ByteArrayInputStream(content), rs.getLoadOptions());
        return resource;
    }

    static EPackage rootPackage(Resource resource, String fileName) {
        EObject root = resource.getContents().isEmpty() ? null : resource.getContents().get(0);
        if (!(root instanceof EPackage ePackage)) {
            throw new InvalidArtifactException("El archivo '" + fileName + "' no contiene un EPackage en su raíz");
        }
        return ePackage;
    }

    /** Registra el paquete y sus subpaquetes por nsURI en el registro dado. */
    static void register(EPackage.Registry registry, EPackage ePackage) {
        if (ePackage.getNsURI() != null) {
            registry.put(ePackage.getNsURI(), ePackage);
        }
        ePackage.getESubpackages().forEach(sub -> register(registry, sub));
    }

    static URI fileUri(Path path) {
        return URI.createFileURI(path.toAbsolutePath().toString());
    }

    static Path createWorkDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw new ModelingException("No se pudo crear el directorio de trabajo temporal", e);
        }
    }

    static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
        } catch (IOException ignored) {
            // Limpieza de temporales en el mejor esfuerzo.
        }
    }

    /** Mensaje más específico de la cadena de causas, para mostrarlo al usuario. */
    static String rootMessage(Throwable error) {
        String message = null;
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t.getMessage() != null && !t.getMessage().isBlank()) {
                message = t.getMessage();
            }
        }
        if (message == null) {
            message = error.getClass().getSimpleName();
        }
        return message.length() > 2000 ? message.substring(0, 2000) + "..." : message;
    }
}
