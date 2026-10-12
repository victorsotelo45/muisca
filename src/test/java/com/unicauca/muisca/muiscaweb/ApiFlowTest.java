package com.unicauca.muisca.muiscaweb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta de la API sobre una base en memoria: arranque con el catálogo sembrado,
 * ejecuciones síncronas y en segundo plano, historial paginado, biblioteca de modelos, ejemplos,
 * cuenta propia y configuración del sistema.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private final Map<String, Long> transformations = new HashMap<>();

    @Test
    void completeFlowForAdminAndUser() throws Exception {
        String admin = login("admin", "admin123");

        // Catálogo sembrado por CatalogSeeder: 4 transformaciones publicadas.
        JsonNode catalog = body(perform(get("/api/transformations"), admin, 200));
        assertThat(catalog.size()).isEqualTo(4);
        catalog.forEach(t -> transformations.put(t.get("name").asText(), t.get("id").asLong()));
        long goToJava = idStartingWith("Contrato Go");
        long solidity = idStartingWith("Generar código Solidity");

        // Ejecución síncrona: 201 con el modelo transformado y el código Java.
        JsonNode sync = body(perform(multipart("/api/transformations/{id}/executions", goToJava)
                .file(sample("SCGo.xmi")), admin, 201));
        assertThat(sync.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(sync.get("outputModelName").asText()).isEqualTo("SCGo-Go2Java.xmi");

        // Ejecución en segundo plano: 202 RUNNING y luego SUCCEEDED.
        JsonNode async = body(perform(multipart("/api/transformations/{id}/executions", solidity)
                .file(sample("SContractPatientSol.xmi")).param("async", "true"), admin, 202));
        assertThat(async.get("status").asText()).isEqualTo("RUNNING");
        JsonNode finished = waitUntilFinished(async.get("id").asLong(), admin);
        assertThat(finished.get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(finished.get("generatedFiles").get(0).asText()).endsWith(".sol");

        // Modelo de otro metamodelo: 400 con explicación, sin registrar ejecución.
        perform(multipart("/api/transformations/{id}/executions", solidity).file(sample("SCGo.xmi")), admin, 400);

        // Historial paginado en el servidor.
        JsonNode page = body(perform(get("/api/executions/search").param("size", "1").param("status", "SUCCEEDED"),
                admin, 200));
        assertThat(page.get("totalElements").asLong()).isEqualTo(2);
        assertThat(page.get("totalPages").asInt()).isEqualTo(2);
        assertThat(page.get("content").size()).isEqualTo(1);

        // Modelos de ejemplo de metamodelos registrados.
        JsonNode samples = body(perform(get("/api/samples"), admin, 200));
        assertThat(samples.findValuesAsText("fileName")).contains("SCGo.xmi", "SContractPatientSol.xmi");
        perform(get("/api/samples/SCGo.xmi"), admin, 200);
        perform(get("/api/samples/noexiste.xmi"), admin, 404);
        // El firewall de Spring Security rechaza rutas con barras codificadas antes de llegar al controlador.
        perform(get("/api/samples/..%2Fapplication.properties"), admin, 400);

        // Usuario nuevo: biblioteca de modelos y ejecución desde un modelo guardado.
        perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ana\",\"password\":\"secreto1\"}"), null, 201);
        String ana = login("ana", "secreto1");
        JsonNode model = body(perform(multipart("/api/models").file(new MockMultipartFile("modelFile", "SCGo.xmi",
                "application/xml", read("SCGo.xmi"))), ana, 201));
        assertThat(model.get("metamodel").get("name").asText()).isEqualTo("HyperledgerGo");
        JsonNode fromLibrary = body(perform(multipart("/api/transformations/{id}/executions", goToJava)
                .param("modelId", model.get("id").asText()), ana, 201));
        assertThat(fromLibrary.get("status").asText()).isEqualTo("SUCCEEDED");
        JsonNode saved = body(perform(post("/api/models/from-execution/{id}", fromLibrary.get("id").asLong()), ana, 201));
        assertThat(saved.get("metamodel").get("name").asText()).isEqualTo("HyperledgerJava");

        // Un usuario solo ve su propio historial.
        JsonNode anaPage = body(perform(get("/api/executions/search"), ana, 200));
        assertThat(anaPage.get("totalElements").asLong()).isEqualTo(1);
        perform(get("/api/executions/{id}", sync.get("id").asLong()), ana, 404);

        // Configuración: con el registro cerrado nadie se puede registrar.
        perform(put("/api/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"registrationEnabled\":false,\"announcement\":\"Mantenimiento\"}"), admin, 200);
        perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"luis\",\"password\":\"secreto1\"}"), null, 403);
        assertThat(body(perform(get("/api/settings/public"), null, 200)).get("announcement").asText())
                .isEqualTo("Mantenimiento");

        // Cuenta propia: cambio de contraseña y eliminación con su historial y biblioteca.
        perform(put("/api/account/password").contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"secreto1\",\"newPassword\":\"nueva123\"}"), ana, 204);
        String ana2 = login("ana", "nueva123");
        perform(delete("/api/account").contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"nueva123\"}"), ana2, 204);
        perform(get("/auth/validate"), ana2, 401);
    }

    private long idStartingWith(String prefix) {
        return transformations.entrySet().stream().filter(e -> e.getKey().startsWith(prefix)).findFirst()
                .orElseThrow().getValue();
    }

    private JsonNode waitUntilFinished(long id, String token) throws Exception {
        for (int i = 0; i < 120; i++) {
            JsonNode execution = body(perform(get("/api/executions/{id}", id), token, 200));
            if (!"RUNNING".equals(execution.get("status").asText())) {
                return execution;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("La ejecución " + id + " no terminó a tiempo");
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"), null, 200);
        return body(result).get("token").asText();
    }

    private MvcResult perform(MockHttpServletRequestBuilder request, String token, int expectedStatus)
            throws Exception {
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
    }

    private JsonNode body(MvcResult result) throws IOException {
        return json.readTree(result.getResponse().getContentAsByteArray());
    }

    private static MockMultipartFile sample(String name) throws IOException {
        return new MockMultipartFile("inputModel", name, "application/xml", read(name));
    }

    private static byte[] read(String name) throws IOException {
        return new ClassPathResource("mde/models/" + name).getContentAsByteArray();
    }
}
