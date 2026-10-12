package com.unicauca.muisca.infrastructure.mde;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.application.port.CodeGenerationEngine;
import com.unicauca.muisca.application.port.ModelingException;
import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.Metamodel;

import org.eclipse.acceleo.engine.generation.strategy.PreviewStrategy;
import org.eclipse.acceleo.engine.service.AcceleoService;
import org.eclipse.acceleo.model.mtl.Module;
import org.eclipse.acceleo.model.mtl.MtlPackage;
import org.eclipse.acceleo.model.mtl.Template;
import org.eclipse.acceleo.model.mtl.resource.EMtlResourceFactoryImpl;
import org.eclipse.acceleo.parser.AcceleoParser;
import org.eclipse.acceleo.parser.AcceleoSourceBuffer;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compila y ejecuta plantillas Acceleo 3 (.mtl) sin Eclipse. Acceleo y OCL resuelven los
 * metamodelos en el registro global de EPackages, por eso compilación y generación se
 * serializan con un candado; los nsURI son únicos en el catálogo, así que registrar el
 * metamodelo de cada petición no pisa a otro distinto.
 */
@Component
public class AcceleoCodeGenerationEngine implements CodeGenerationEngine {

    private static final Pattern MODULE = Pattern.compile("\\[module\\s+([\\w.]+)\\s*\\(([^)]*)\\)");
    private static final Pattern QUOTED = Pattern.compile("'([^']*)'");
    private static final String STDLIB_BASE = "http://www.eclipse.org/acceleo/mtl/3.0/";
    private static final Object LOCK = new Object();
    private static final Path GENERATION_ROOT = Path.of(System.getProperty("java.io.tmpdir"), "muisca-generation")
            .toAbsolutePath().normalize();

    private record ParsedTemplate(Module module, EPackage ePackage, String moduleName, List<String> mainTemplates,
            List<ArtifactProblem> problems) {

        boolean hasErrors() {
            return problems.stream().anyMatch(ArtifactProblem::isError);
        }
    }

    public AcceleoCodeGenerationEngine() {
        registerStandaloneEnvironment();
    }

    @Override
    public TemplateCompilation compile(String templateSource, Metamodel metamodel) {
        synchronized (LOCK) {
            ParsedTemplate parsed = parse(templateSource, metamodel);
            return new TemplateCompilation(parsed.moduleName(), String.join(", ", parsed.mainTemplates()),
                    parsed.problems());
        }
    }

    @Override
    public Map<String, byte[]> generate(AcceleoModule acceleoModule, byte[] model) {
        synchronized (LOCK) {
            ParsedTemplate parsed = parse(acceleoModule.getTemplateSource(), acceleoModule.getMetamodel());
            if (parsed.hasErrors()) {
                throw new ModelingException("La plantilla Acceleo '" + acceleoModule.getName() + "' tiene errores: "
                        + parsed.problems().get(0).message());
            }
            try {
                ResourceSet rs = EmfSupport.newResourceSet();
                EmfSupport.register(rs.getPackageRegistry(), parsed.ePackage());
                Resource modelResource = EmfSupport.load(rs, "model.xmi", model);
                EObject root = modelResource.getContents().get(0);

                // Con PreviewStrategy Acceleo no escribe en disco: los nombres de archivo salen
                // de datos del modelo que sube el usuario y podrían apuntar fuera del directorio.
                Map<String, byte[]> files = new LinkedHashMap<>();
                for (String template : parsed.mainTemplates()) {
                    Map<String, String> preview = new AcceleoService(new PreviewStrategy()).doGenerate(
                            parsed.module(), template, root, new ArrayList<>(), GENERATION_ROOT.toFile(),
                            new BasicMonitor());
                    preview.forEach((path, content) -> files.put(relativePath(path),
                            content.getBytes(StandardCharsets.UTF_8)));
                }
                if (files.isEmpty()) {
                    throw new ModelingException("La plantilla '" + acceleoModule.getName() + "' no generó ningún "
                            + "archivo: la raíz del modelo (" + root.eClass().getName() + ") no es del tipo que "
                            + "espera la plantilla principal");
                }
                return files;
            } catch (ModelingException e) {
                throw e;
            } catch (Exception | LinkageError e) {
                throw new ModelingException("Error al generar código con Acceleo: " + EmfSupport.rootMessage(e), e);
            }
        }
    }

    private static String relativePath(String generatedPath) {
        Path normalized = GENERATION_ROOT.resolve(generatedPath).normalize();
        if (!normalized.startsWith(GENERATION_ROOT)) {
            throw new ModelingException("La plantilla intentó generar un archivo fuera del directorio de salida: "
                    + generatedPath);
        }
        return GENERATION_ROOT.relativize(normalized).toString().replace('\\', '/');
    }

    private ParsedTemplate parse(String source, Metamodel metamodel) {
        List<ArtifactProblem> problems = new ArrayList<>();
        Matcher header = MODULE.matcher(source);
        if (!header.find()) {
            problems.add(ArtifactProblem.error(1, "Falta la declaración [module nombre('nsURI')]"));
            return new ParsedTemplate(null, null, null, List.of(), problems);
        }
        String moduleName = header.group(1);
        List<String> nsUris = new ArrayList<>();
        for (Matcher m = QUOTED.matcher(header.group(2)); m.find();) {
            nsUris.add(m.group(1));
        }
        if (!nsUris.contains(metamodel.getNsUri())) {
            problems.add(ArtifactProblem.error(1, "La plantilla declara los metamodelos " + nsUris + " pero el "
                    + "metamodelo seleccionado es " + metamodel.getName() + " (" + metamodel.getNsUri() + ")"));
        }
        nsUris.stream()
                .filter(uri -> !uri.equals(metamodel.getNsUri()) && !uri.equals(EcorePackage.eNS_URI))
                .forEach(uri -> problems.add(ArtifactProblem.error(1,
                        "Solo se admite un metamodelo por plantilla; sobra '" + uri + "'")));
        if (!problems.isEmpty()) {
            return new ParsedTemplate(null, null, moduleName, List.of(), problems);
        }

        Path workDir = EmfSupport.createWorkDir("muisca-mtl-");
        try {
            EPackage ePackage = EmfSupport.rootPackage(
                    EmfSupport.load(EmfSupport.newResourceSet(), metamodel.getEcoreFileName(),
                            metamodel.getEcoreData()),
                    metamodel.getEcoreFileName());
            EmfSupport.register(EPackage.Registry.INSTANCE, ePackage);

            // El parser de Acceleo exige que el archivo se llame igual que el módulo.
            Path mtlFile = workDir.resolve(moduleName + ".mtl");
            Files.writeString(mtlFile, source, StandardCharsets.UTF_8);
            AcceleoSourceBuffer buffer = new AcceleoSourceBuffer(mtlFile.toFile());
            ResourceSet mtlResourceSet = new ResourceSetImpl();
            mtlResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                    .put("emtl", new EMtlResourceFactoryImpl());
            Resource emtl = mtlResourceSet.createResource(URI.createFileURI(
                    workDir.resolve(moduleName + ".emtl").toString()));
            new AcceleoParser().parse(buffer, emtl, new ArrayList<>());

            buffer.getProblems().getList()
                    .forEach(p -> problems.add(ArtifactProblem.error(p.getLine(), p.getMessage())));
            buffer.getWarnings().getList()
                    .forEach(w -> problems.add(ArtifactProblem.warning(w.getLine(), w.getMessage())));

            Module module = emtl.getContents().isEmpty() ? null : (Module) emtl.getContents().get(0);
            List<String> mains = new ArrayList<>();
            if (module != null) {
                module.getOwnedModuleElement().stream()
                        .filter(e -> e instanceof Template t && t.isMain())
                        .forEach(e -> mains.add(((Template) e).getName()));
            }
            if (mains.isEmpty() && problems.stream().noneMatch(ArtifactProblem::isError)) {
                problems.add(ArtifactProblem.error(null,
                        "Ninguna plantilla está marcada como principal con [comment @main/]"));
            }
            return new ParsedTemplate(module, ePackage, moduleName, mains, problems);
        } catch (Exception | LinkageError e) {
            problems.add(ArtifactProblem.error(null, "No se pudo compilar la plantilla: " + EmfSupport.rootMessage(e)));
            return new ParsedTemplate(null, null, moduleName, List.of(), problems);
        } finally {
            EmfSupport.deleteQuietly(workDir);
        }
    }

    /**
     * Fuera de Eclipse no existen los "uri_mapping" de los plugins: se copian las librerías
     * estándar de Acceleo a archivos temporales y se mapean sus URIs, y se registran los
     * paquetes de MTL y OCL.
     */
    private static void registerStandaloneEnvironment() {
        synchronized (LOCK) {
            EPackage.Registry.INSTANCE.put(MtlPackage.eNS_URI, MtlPackage.eINSTANCE);
            EPackage.Registry.INSTANCE.put(org.eclipse.ocl.ecore.EcorePackage.eNS_URI,
                    org.eclipse.ocl.ecore.EcorePackage.eINSTANCE);
            EPackage.Registry.INSTANCE.put(org.eclipse.ocl.expressions.ExpressionsPackage.eNS_URI,
                    org.eclipse.ocl.expressions.ExpressionsPackage.eINSTANCE);
            for (String library : List.of("mtlstdlib", "mtlnonstdlib")) {
                URI uri = URI.createURI(STDLIB_BASE + library + ".ecore");
                if (URIConverter.URI_MAP.containsKey(uri)) {
                    continue;
                }
                try (InputStream in = MtlPackage.class.getClassLoader()
                        .getResourceAsStream("model/" + library + ".ecore")) {
                    if (in == null) {
                        throw new IllegalStateException("No se encontró model/" + library + ".ecore en el classpath");
                    }
                    Path copy = Files.createTempFile("acceleo-" + library, ".ecore");
                    copy.toFile().deleteOnExit();
                    Files.copy(in, copy, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    URIConverter.URI_MAP.put(uri, URI.createFileURI(copy.toString()));
                } catch (IOException e) {
                    throw new IllegalStateException("No se pudo preparar la librería estándar de Acceleo", e);
                }
            }
        }
    }
}
