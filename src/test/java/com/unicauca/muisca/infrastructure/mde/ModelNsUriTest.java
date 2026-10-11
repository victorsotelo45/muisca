package com.unicauca.muisca.infrastructure.mde;

import com.unicauca.muisca.application.exception.InvalidArtifactException;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Detección del metamodelo de un modelo por el nsURI de su raíz (biblioteca de modelos). */
class ModelNsUriTest {

    private final EmfMetamodelInspector inspector = new EmfMetamodelInspector();

    @Test
    void detectsTheNsUriOfTheSampleModels() throws IOException {
        assertThat(inspector.modelNsUri("SCGo.xmi", bytes("mde/models/SCGo.xmi")))
                .isEqualTo("http://www.example.org/hyperledgerGo");
        assertThat(inspector.modelNsUri("SCJava.xmi", bytes("mde/models/SCJava.xmi")))
                .isEqualTo("http://www.example.org/hyperledgerJava");
    }

    @Test
    void skipsTheXmiWrapperElement() {
        String model = "<xmi:XMI xmi:version=\"2.0\" xmlns:xmi=\"http://www.omg.org/XMI\" xmlns:a=\"urn:a\">"
                + "<a:Root/></xmi:XMI>";
        assertThat(inspector.modelNsUri("m.xmi", model.getBytes(StandardCharsets.UTF_8))).isEqualTo("urn:a");
    }

    @Test
    void rejectsModelsWithoutNamespace() {
        assertThatThrownBy(() -> inspector.modelNsUri("m.xmi", "<Root/>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidArtifactException.class)
                .hasMessageContaining("nsURI");
    }

    @Test
    void rejectsDoctypeDeclarations() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><r>&x;</r>";
        assertThatThrownBy(() -> inspector.modelNsUri("m.xmi", xxe.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(InvalidArtifactException.class);
    }

    private static byte[] bytes(String path) throws IOException {
        return new ClassPathResource(path).getContentAsByteArray();
    }
}
