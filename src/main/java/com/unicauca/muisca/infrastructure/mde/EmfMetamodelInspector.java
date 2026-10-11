package com.unicauca.muisca.infrastructure.mde;

import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.application.port.MetamodelInspector;
import com.unicauca.muisca.domain.model.Metamodel;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.springframework.stereotype.Component;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.util.List;

@Component
public class EmfMetamodelInspector implements MetamodelInspector {

    @Override
    public EcoreInfo inspect(String fileName, byte[] ecore) {
        EmfSupport.requireSafeXml(fileName, ecore);
        EPackage ePackage;
        try {
            ePackage = EmfSupport.rootPackage(EmfSupport.load(EmfSupport.newResourceSet(), fileName, ecore), fileName);
        } catch (InvalidArtifactException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidArtifactException("No se pudo leer el metamodelo '" + fileName + "': "
                    + EmfSupport.rootMessage(e));
        }
        if (ePackage.getNsURI() == null || ePackage.getNsURI().isBlank()) {
            throw new InvalidArtifactException("El metamodelo '" + fileName + "' no define un nsURI");
        }
        // Eclipse también marca estos hallazgos (p. ej. literales como "[]int"), pero no impiden
        // usar el metamodelo en ATL ni en Acceleo; se informan sin bloquear el registro.
        List<ArtifactProblem> warnings = Diagnostician.INSTANCE.validate(ePackage).getChildren().stream()
                .filter(d -> d.getSeverity() >= Diagnostic.WARNING)
                .map(d -> ArtifactProblem.warning(null, d.getMessage()))
                .toList();
        return new EcoreInfo(ePackage.getName(), ePackage.getNsURI(), warnings);
    }

    @Override
    public void validateModel(Metamodel metamodel, String fileName, byte[] model) {
        EmfSupport.requireSafeXml(fileName, model);
        ResourceSet rs = EmfSupport.newResourceSet();
        Resource modelResource;
        try {
            EPackage ePackage = EmfSupport.rootPackage(
                    EmfSupport.load(rs, metamodel.getEcoreFileName(), metamodel.getEcoreData()),
                    metamodel.getEcoreFileName());
            EmfSupport.register(rs.getPackageRegistry(), ePackage);
            modelResource = EmfSupport.load(rs, fileName, model);
        } catch (Exception e) {
            throw new InvalidArtifactException("El modelo '" + fileName + "' no es conforme al metamodelo "
                    + metamodel.getName() + ": " + EmfSupport.rootMessage(e));
        }
        if (modelResource.getContents().isEmpty()) {
            throw new InvalidArtifactException("El modelo '" + fileName + "' está vacío");
        }
        EObject root = modelResource.getContents().get(0);
        String rootNsUri = root.eClass().getEPackage().getNsURI();
        if (!metamodel.getNsUri().equals(rootNsUri)) {
            throw new InvalidArtifactException("El modelo '" + fileName + "' pertenece al metamodelo '" + rootNsUri
                    + "', pero esta transformación espera un modelo de " + metamodel.getName() + " ("
                    + metamodel.getNsUri() + ")");
        }
    }

    @Override
    public String modelNsUri(String fileName, byte[] model) {
        EmfSupport.requireSafeXml(fileName, model);
        RootNamespace handler = new RootNamespace();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.newSAXParser().parse(new ByteArrayInputStream(model), handler);
        } catch (RootFound found) {
            return found.nsUri;
        } catch (InvalidArtifactException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidArtifactException("No se pudo leer el modelo '" + fileName + "': "
                    + EmfSupport.rootMessage(e));
        }
        throw new InvalidArtifactException("El modelo '" + fileName + "' está vacío");
    }

    /** Detiene la lectura en el primer elemento que no sea el contenedor xmi:XMI. */
    private static final class RootNamespace extends DefaultHandler {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            boolean xmiWrapper = "XMI".equals(localName) && uri != null && uri.startsWith("http://www.omg.org/");
            if (xmiWrapper) {
                return;
            }
            if (uri == null || uri.isBlank()) {
                throw new InvalidArtifactException("La raíz del modelo <" + qName + "> no declara el espacio de "
                        + "nombres (nsURI) de su metamodelo");
            }
            throw new RootFound(uri);
        }
    }

    private static final class RootFound extends SAXException {
        private final String nsUri;

        RootFound(String nsUri) {
            super(nsUri);
            this.nsUri = nsUri;
        }
    }
}
