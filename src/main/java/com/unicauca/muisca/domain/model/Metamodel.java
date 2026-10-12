package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import org.hibernate.Length;
import java.time.LocalDateTime;

/**
 * Metamodelo Ecore de una plataforma (p. ej. HyperledgerGo). El nsURI es único porque los
 * motores ATL y Acceleo resuelven los metamodelos por nsURI.
 */
@Entity
@Table(name = "metamodels")
public class Metamodel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "ns_uri", nullable = false, unique = true)
    private String nsUri;

    @Column(name = "ecore_file_name", nullable = false)
    private String ecoreFileName;

    @Column(name = "ecore_data", nullable = false, length = Length.LONG32)
    private byte[] ecoreData;

    @Column(name = "genmodel_data", length = Length.LONG32)
    private byte[] genmodelData;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Metamodel() {
    }

    public Metamodel(String name, String description, String nsUri, String ecoreFileName, byte[] ecoreData,
            byte[] genmodelData, UserEntity createdBy) {
        this.name = name;
        this.description = description;
        this.nsUri = nsUri;
        this.ecoreFileName = ecoreFileName;
        this.ecoreData = ecoreData;
        this.genmodelData = genmodelData;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    public void describe(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getNsUri() {
        return nsUri;
    }

    public String getEcoreFileName() {
        return ecoreFileName;
    }

    public byte[] getEcoreData() {
        return ecoreData;
    }

    public byte[] getGenmodelData() {
        return genmodelData;
    }

    public UserEntity getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
