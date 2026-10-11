package com.unicauca.muisca.infrastructure.mde;

import com.unicauca.muisca.application.port.ArtifactProblem;
import com.unicauca.muisca.application.port.ModelTransformationEngine;
import com.unicauca.muisca.application.port.ModelingException;
import com.unicauca.muisca.domain.model.AtlCompiler;
import com.unicauca.muisca.domain.model.AtlModule;
import com.unicauca.muisca.domain.model.Metamodel;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.m2m.atl.core.IModel;
import org.eclipse.m2m.atl.core.IReferenceModel;
import org.eclipse.m2m.atl.core.emf.EMFExtractor;
import org.eclipse.m2m.atl.core.emf.EMFInjector;
import org.eclipse.m2m.atl.core.emf.EMFModelFactory;
import org.eclipse.m2m.atl.core.launch.ILauncher;
import org.eclipse.m2m.atl.emftvm.EmftvmFactory;
import org.eclipse.m2m.atl.emftvm.ExecEnv;
import org.eclipse.m2m.atl.emftvm.Model;
import org.eclipse.m2m.atl.emftvm.compiler.AtlToEmftvmCompiler;
import org.eclipse.m2m.atl.emftvm.impl.resource.EMFTVMResourceFactoryImpl;
import org.eclipse.m2m.atl.emftvm.util.DefaultModuleResolver;
import org.eclipse.m2m.atl.emftvm.util.TimingData;
import org.eclipse.m2m.atl.engine.compiler.AtlStandaloneCompiler;
import org.eclipse.m2m.atl.engine.compiler.atl2006.Atl2006Compiler;
import org.eclipse.m2m.atl.engine.emfvm.launch.EMFVMLauncher;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compila y ejecuta módulos ATL sin Eclipse: los marcados con {@code @atlcompiler emftvm} en
 * EMFTVM y el resto con el compilador ATL2006 sobre EMFVM, igual que hace el IDE.
 */
@Component
public class AtlTransformationEngine implements ModelTransformationEngine {

    private static final Pattern LOCATION_LINE = Pattern.compile("^(\\d+):");
    private static final Object COMPILER_LOCK = new Object();

    @Override
    public AtlCompilation compile(String atlSource) {
        AtlHeader header = AtlHeader.parse(atlSource);
        List<ArtifactProblem> problems = new ArrayList<>(header.problems());
        if (problems.stream().anyMatch(ArtifactProblem::isError)) {
            return new AtlCompilation(header.compiler(), header.moduleName(), header.inModelName(),
                    header.sourceAlias(), header.outModelName(), header.targetAlias(), null, problems);
        }

        AtlStandaloneCompiler compiler = header.compiler() == AtlCompiler.EMFTVM
                ? new AtlToEmftvmCompiler()
                : new Atl2006Compiler();
        ByteArrayOutputStream compiled = new ByteArrayOutputStream();
        try {
            EObject[] compilerProblems;
            synchronized (COMPILER_LOCK) {
                compilerProblems = compiler.compileWithProblemModel(new StringReader(atlSource), compiled);
            }
            for (EObject problem : compilerProblems) {
                problems.add(toProblem(problem));
            }
        } catch (RuntimeException e) {
            problems.add(ArtifactProblem.error(null, "El compilador ATL falló: " + EmfSupport.rootMessage(e)));
        }
        if (compiled.size() == 0 && problems.stream().noneMatch(ArtifactProblem::isError)) {
            problems.add(ArtifactProblem.error(null, "El compilador ATL no produjo ningún módulo"));
        }
        return new AtlCompilation(header.compiler(), header.moduleName(), header.inModelName(), header.sourceAlias(),
                header.outModelName(), header.targetAlias(), compiled.toByteArray(), problems);
    }

    @Override
    public byte[] transform(AtlModule module, byte[] inputModel) {
        Path dir = EmfSupport.createWorkDir("muisca-atl-");
        try {
            Path sourceEcore = dir.resolve("source.ecore");
            Path targetEcore = dir.resolve("target.ecore");
            Path input = dir.resolve("input.xmi");
            Path output = dir.resolve("output.xmi");
            Files.write(sourceEcore, module.getSourceMetamodel().getEcoreData());
            Files.write(targetEcore, module.getTargetMetamodel().getEcoreData());
            Files.write(input, inputModel);
            boolean endogenous = sameMetamodel(module.getSourceMetamodel(), module.getTargetMetamodel());

            if (module.getCompiler() == AtlCompiler.EMFTVM) {
                runEmftvm(module, dir, sourceEcore, endogenous ? sourceEcore : targetEcore, input, output);
            } else {
                runEmfvm(module, sourceEcore, endogenous ? sourceEcore : targetEcore, input, output);
            }
            return Files.readAllBytes(output);
        } catch (ModelingException e) {
            throw e;
        } catch (Exception | LinkageError e) {
            throw new ModelingException("Error al ejecutar la transformación ATL '" + module.getModuleName() + "': "
                    + EmfSupport.rootMessage(e), e);
        } finally {
            EmfSupport.deleteQuietly(dir);
        }
    }

    private void runEmftvm(AtlModule module, Path dir, Path sourceEcore, Path targetEcore, Path input, Path output)
            throws Exception {
        ResourceSet rs = EmfSupport.newResourceSet();
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("emftvm", new EMFTVMResourceFactoryImpl());
        ExecEnv env = EmftvmFactory.eINSTANCE.createExecEnv();
        env.registerMetaModel(module.getSourceAlias(), emftvmMetamodel(rs, sourceEcore));
        env.registerMetaModel(module.getTargetAlias(), emftvmMetamodel(rs, targetEcore));

        Model in = EmftvmFactory.eINSTANCE.createModel();
        in.setResource(rs.getResource(EmfSupport.fileUri(input), true));
        env.registerInputModel(module.getInModelName(), in);

        Model out = EmftvmFactory.eINSTANCE.createModel();
        out.setResource(rs.createResource(EmfSupport.fileUri(output)));
        env.registerOutputModel(module.getOutModelName(), out);

        Files.write(dir.resolve(module.getModuleName() + ".emftvm"), module.getCompiledModule());
        env.loadModule(new DefaultModuleResolver(dir.toUri().toString(), rs), module.getModuleName());
        env.run(new TimingData());
        out.getResource().save(Collections.emptyMap());
    }

    private org.eclipse.m2m.atl.emftvm.Metamodel emftvmMetamodel(ResourceSet rs, Path ecore) {
        Resource resource = rs.getResource(EmfSupport.fileUri(ecore), true);
        EPackage ePackage = EmfSupport.rootPackage(resource, ecore.getFileName().toString());
        EmfSupport.register(rs.getPackageRegistry(), ePackage);
        org.eclipse.m2m.atl.emftvm.Metamodel metamodel = EmftvmFactory.eINSTANCE.createMetamodel();
        metamodel.setResource(resource);
        return metamodel;
    }

    private void runEmfvm(AtlModule module, Path sourceEcore, Path targetEcore, Path input, Path output)
            throws Exception {
        EMFModelFactory factory = new EMFModelFactory();
        EMFInjector injector = new EMFInjector();
        IReferenceModel sourceMetamodel = factory.newReferenceModel();
        injector.inject(sourceMetamodel, EmfSupport.fileUri(sourceEcore).toString());
        IReferenceModel targetMetamodel = sourceMetamodel;
        if (!sourceEcore.equals(targetEcore)) {
            targetMetamodel = factory.newReferenceModel();
            injector.inject(targetMetamodel, EmfSupport.fileUri(targetEcore).toString());
        }
        IModel in = factory.newModel(sourceMetamodel);
        injector.inject(in, EmfSupport.fileUri(input).toString());
        IModel out = factory.newModel(targetMetamodel);

        EMFVMLauncher launcher = new EMFVMLauncher();
        launcher.initialize(new HashMap<>());
        launcher.addInModel(in, module.getInModelName(), module.getSourceAlias());
        launcher.addOutModel(out, module.getOutModelName(), module.getTargetAlias());
        Map<String, Object> options = new HashMap<>();
        options.put("allowInterModelReferences", "true");
        launcher.launch(ILauncher.RUN_MODE, new NullProgressMonitor(), options,
                new ByteArrayInputStream(module.getCompiledModule()));
        new EMFExtractor().extract(out, EmfSupport.fileUri(output).toString());
    }

    private static boolean sameMetamodel(Metamodel a, Metamodel b) {
        return a.getNsUri().equals(b.getNsUri());
    }

    private static ArtifactProblem toProblem(EObject problem) {
        String severity = String.valueOf(get(problem, "severity"));
        String location = String.valueOf(get(problem, "location"));
        String description = String.valueOf(get(problem, "description"));
        Integer line = null;
        Matcher m = LOCATION_LINE.matcher(location);
        if (m.find()) {
            line = Integer.valueOf(m.group(1));
        }
        return "error".equalsIgnoreCase(severity) ? ArtifactProblem.error(line, description)
                : ArtifactProblem.warning(line, description);
    }

    private static Object get(EObject object, String feature) {
        EStructuralFeature f = object.eClass().getEStructuralFeature(feature);
        Object value = f == null ? null : object.eGet(f);
        return value instanceof Enumerator literal ? literal.getLiteral() : value;
    }
}
