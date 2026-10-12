package com.unicauca.muisca.infrastructure.config;

import com.unicauca.muisca.application.dto.AcceleoModuleResponse;
import com.unicauca.muisca.application.dto.AtlModuleResponse;
import com.unicauca.muisca.application.dto.MetamodelResponse;
import com.unicauca.muisca.application.dto.TransformationRequest;
import com.unicauca.muisca.application.dto.TransformationResponse;
import com.unicauca.muisca.application.dto.UploadedFile;
import com.unicauca.muisca.application.service.AcceleoModuleService;
import com.unicauca.muisca.application.service.AtlModuleService;
import com.unicauca.muisca.application.service.MetamodelService;
import com.unicauca.muisca.application.service.TransformationService;
import com.unicauca.muisca.domain.model.Role;
import com.unicauca.muisca.domain.model.UserEntity;
import com.unicauca.muisca.domain.repository.MetamodelRepository;
import com.unicauca.muisca.domain.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Siembra el catálogo inicial con los artefactos del Grupo IDIS incluidos en
 * src/main/resources/mde, pasando por los mismos servicios (validación y compilación) que la
 * API. Las transformaciones IContract* no se siembran porque falta su metamodelo PIM
 * (sclang.ecore); un ingeniero de modelos puede registrarlas cuando se recupere.
 */
@Component
@Order(2)
public class CatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);
    private static final String BASE = "mde/";

    private final MetamodelRepository metamodelRepository;
    private final UserRepository userRepository;
    private final MetamodelService metamodelService;
    private final AtlModuleService atlModuleService;
    private final AcceleoModuleService acceleoModuleService;
    private final TransformationService transformationService;

    public CatalogSeeder(MetamodelRepository metamodelRepository, UserRepository userRepository,
            MetamodelService metamodelService, AtlModuleService atlModuleService,
            AcceleoModuleService acceleoModuleService, TransformationService transformationService) {
        this.metamodelRepository = metamodelRepository;
        this.userRepository = userRepository;
        this.metamodelService = metamodelService;
        this.atlModuleService = atlModuleService;
        this.acceleoModuleService = acceleoModuleService;
        this.transformationService = transformationService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (metamodelRepository.count() > 0) {
            return;
        }
        UserEntity admin = userRepository.findFirstByRole(Role.ADMIN).orElse(null);
        if (admin == null) {
            log.warn("No hay ningún usuario ADMIN: se omite la siembra del catálogo inicial (se reintentará en el "
                    + "próximo arranque).");
            return;
        }

        MetamodelResponse go = metamodel("HyperledgerGo",
                "Contratos inteligentes para Hyperledger Fabric en Go", "HyperledgerGo/hyperledgerGo", admin);
        MetamodelResponse java = metamodel("HyperledgerJava",
                "Contratos inteligentes para Hyperledger Fabric en Java", "HyperledgerJava/hyperledgerJava", admin);
        MetamodelResponse solidity = metamodel("EthereumSolidity",
                "Contratos inteligentes para Ethereum en Solidity", "EthereumSolidity/ethereumSolidity", admin);

        AtlModuleResponse go2java = atlModuleService.create("Go2Java",
                "Transforma un contrato de Hyperledger Fabric en Go a su equivalente en Java", go.id(), java.id(),
                file("transformations/atl/Go2Java.atl"), admin);

        AcceleoModuleResponse goCode = acceleoModuleService.create("Generador de código Go",
                "Genera el chaincode Go para Hyperledger Fabric", go.id(),
                file("transformations/acceleo/generateHyperledgerGo.mtl"), admin);
        AcceleoModuleResponse javaCode = acceleoModuleService.create("Generador de código Java",
                "Genera el contrato Java para Hyperledger Fabric", java.id(),
                file("transformations/acceleo/generateContractJava.mtl"), admin);
        AcceleoModuleResponse solidityCode = acceleoModuleService.create("Generador de código Solidity",
                "Genera el contrato Solidity para Ethereum", solidity.id(),
                file("transformations/acceleo/generateEthereumSolidity.mtl"), admin);

        publish(new TransformationRequest("Contrato Go → contrato Java (Hyperledger Fabric)",
                "Sube un modelo de contrato Go y obtén el modelo y el código del contrato equivalente en Java",
                go.id(), go2java.id(), javaCode.id()), admin);
        publish(new TransformationRequest("Generar código Go (Hyperledger Fabric)",
                "Sube un modelo de contrato HyperledgerGo y obtén el chaincode en Go", go.id(), null, goCode.id()),
                admin);
        publish(new TransformationRequest("Generar código Java (Hyperledger Fabric)",
                "Sube un modelo de contrato HyperledgerJava y obtén el contrato en Java", java.id(), null,
                javaCode.id()), admin);
        publish(new TransformationRequest("Generar código Solidity (Ethereum)",
                "Sube un modelo de contrato EthereumSolidity y obtén el contrato en Solidity", solidity.id(), null,
                solidityCode.id()), admin);

        log.info("Catálogo inicial sembrado: 3 metamodelos, 1 módulo ATL, 3 módulos Acceleo y 4 transformaciones.");
    }

    private MetamodelResponse metamodel(String name, String description, String path, UserEntity admin)
            throws IOException {
        return metamodelService.create(name, description, file("metamodels/" + path + ".ecore"),
                file("metamodels/" + path + ".genmodel"), admin);
    }

    private void publish(TransformationRequest request, UserEntity admin) {
        TransformationResponse transformation = transformationService.create(request, admin);
        transformationService.setPublished(transformation.id(), true, admin);
    }

    private static UploadedFile file(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(BASE + path);
        return new UploadedFile(resource.getFilename(), resource.getContentAsByteArray());
    }
}
