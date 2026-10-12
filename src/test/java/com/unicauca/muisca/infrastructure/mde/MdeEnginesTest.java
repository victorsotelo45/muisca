package com.unicauca.muisca.infrastructure.mde;

import com.unicauca.muisca.application.exception.InvalidArtifactException;
import com.unicauca.muisca.application.port.CodeGenerationEngine.TemplateCompilation;
import com.unicauca.muisca.application.port.ModelTransformationEngine.AtlCompilation;
import com.unicauca.muisca.application.port.ModelingException;
import com.unicauca.muisca.domain.model.AcceleoModule;
import com.unicauca.muisca.domain.model.AtlCompiler;
import com.unicauca.muisca.domain.model.AtlModule;
import com.unicauca.muisca.domain.model.Metamodel;
import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cadena MDE completa sobre los artefactos de src/main/resources/mde, sin Spring ni base de datos. */
class MdeEnginesTest {

    private final UserEntity user = new UserEntity("tester", "x", Role.ADMIN);
    private final EmfMetamodelInspector inspector = new EmfMetamodelInspector();
    private final AtlTransformationEngine atl = new AtlTransformationEngine();
    private final AcceleoCodeGenerationEngine acceleo = new AcceleoCodeGenerationEngine();

    private final Metamodel go = metamodel("HyperledgerGo", "HyperledgerGo/hyperledgerGo.ecore");
    private final Metamodel java = metamodel("HyperledgerJava", "HyperledgerJava/hyperledgerJava.ecore");
    private final Metamodel solidity = metamodel("EthereumSolidity", "EthereumSolidity/ethereumSolidity.ecore");

    @Test
    void compilesEveryAtlModuleWithTheCompilerDeclaredInItsHeader() {
        AtlCompilation go2java = atl.compile(text("transformations/atl/Go2Java.atl"));
        assertThat(go2java.problems()).noneMatch(p -> p.isError());
        assertThat(go2java.compiler()).isEqualTo(AtlCompiler.EMFTVM);
        assertThat(go2java.moduleName()).isEqualTo("Go2Java");
        assertThat(go2java.sourceAlias()).isEqualTo("Go");
        assertThat(go2java.targetAlias()).isEqualTo("Java");
        assertThat(go2java.compiledModule()).isNotEmpty();

        AtlCompilation toSolidity = atl.compile(text("transformations/atl/IContract2Solidity.atl"));
        assertThat(toSolidity.hasErrors()).isFalse();
        assertThat(toSolidity.compiler()).isEqualTo(AtlCompiler.ATL2006);
        assertThat(toSolidity.sourceAlias()).isEqualTo("IContract");
    }

    @Test
    void reportsAtlSyntaxErrorsWithLineNumbers() {
        AtlCompilation broken = atl.compile(
                "module Broken;\ncreate OUT : B from IN : A;\nrule R {\n from s : A!X\n to t : B!Y (\n name <- s.\n )\n}");
        assertThat(broken.hasErrors()).isTrue();
        assertThat(broken.problems()).anyMatch(p -> p.isError() && Integer.valueOf(7).equals(p.line()));
    }

    @Test
    void goModelBecomesJavaModelAndJavaCode() throws IOException {
        AtlModule go2java = atlModule("transformations/atl/Go2Java.atl", go, java);
        byte[] javaModel = atl.transform(go2java, bytes("models/SCGo.xmi"));
        assertThat(new String(javaModel, StandardCharsets.UTF_8)).contains("hyperledgerJava:SmartContract");
        inspector.validateModel(java, "out.xmi", javaModel);

        Map<String, byte[]> code = acceleo.generate(
                acceleoModule("transformations/acceleo/generateContractJava.mtl", java), javaModel);
        assertThat(code).containsKey("contractEhr.java");
        assertThat(new String(code.get("contractEhr.java"), StandardCharsets.UTF_8))
                .contains("public class contractEhr implements ContractInterface");
    }

    @Test
    void generatesGoAndSolidityCode() throws IOException {
        Map<String, byte[]> goCode = acceleo.generate(
                acceleoModule("transformations/acceleo/generateHyperledgerGo.mtl", go), bytes("models/SCGo.xmi"));
        assertThat(goCode).containsKey("ehr.go");

        Map<String, byte[]> solidityCode = acceleo.generate(
                acceleoModule("transformations/acceleo/generateEthereumSolidity.mtl", solidity),
                bytes("models/SContractPatientSol.xmi"));
        assertThat(solidityCode).hasSize(1);
        assertThat(new String(solidityCode.values().iterator().next(), StandardCharsets.UTF_8))
                .contains("pragma solidity");
    }

    @Test
    void rejectsTemplateWrittenForAnotherMetamodel() {
        TemplateCompilation compilation = acceleo.compile(text("transformations/acceleo/generateContractJava.mtl"), go);
        assertThat(compilation.hasErrors()).isTrue();
    }

    @Test
    void rejectsModelThatDoesNotConformToTheMetamodel() throws IOException {
        assertThatThrownBy(() -> inspector.validateModel(go, "SCJava.xmi", bytes("models/SCJava.xmi")))
                .isInstanceOf(InvalidArtifactException.class)
                .hasMessageContaining("HyperledgerGo");
    }

    @Test
    void rejectsXmlWithDoctypeToPreventXxe() {
        byte[] xxe = ("<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]>"
                + "<hyperledgerGo:SmartContract xmlns:hyperledgerGo=\"http://www.example.org/hyperledgerGo\" "
                + "FileName=\"&e;\"/>").getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> inspector.validateModel(go, "xxe.xmi", xxe))
                .isInstanceOf(InvalidArtifactException.class);
    }

    @Test
    void refusesToGenerateFilesOutsideTheOutputDirectory() throws IOException {
        String model = new String(bytes("models/SCGo.xmi"), StandardCharsets.UTF_8)
                .replace("FileName=\"ehr\"", "FileName=\"../../escape\"");
        assertThat(model).contains("../../escape");
        assertThatThrownBy(() -> acceleo.generate(
                acceleoModule("transformations/acceleo/generateHyperledgerGo.mtl", go),
                model.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(ModelingException.class)
                .hasMessageContaining("fuera del directorio");
    }

    private AtlModule atlModule(String path, Metamodel source, Metamodel target) {
        AtlCompilation c = atl.compile(text(path));
        AtlModule module = new AtlModule(c.moduleName(), null, source, target, user);
        module.applyCompilation(text(path), c.compiler(), c.moduleName(), c.inModelName(), c.sourceAlias(),
                c.outModelName(), c.targetAlias(), c.compiledModule());
        return module;
    }

    private AcceleoModule acceleoModule(String path, Metamodel metamodel) {
        TemplateCompilation c = acceleo.compile(text(path), metamodel);
        assertThat(c.problems()).noneMatch(p -> p.isError());
        AcceleoModule module = new AcceleoModule(c.moduleName(), null, metamodel, user);
        module.applyCompilation(text(path), c.moduleName(), c.mainTemplate());
        return module;
    }

    private Metamodel metamodel(String name, String path) {
        try {
            byte[] ecore = bytes("metamodels/" + path);
            String file = path.substring(path.indexOf('/') + 1);
            return new Metamodel(name, null, inspector.inspect(file, ecore).nsUri(), file, ecore, null, user);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] bytes(String path) throws IOException {
        return new ClassPathResource("mde/" + path).getContentAsByteArray();
    }

    private static String text(String path) {
        try {
            return new String(bytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
