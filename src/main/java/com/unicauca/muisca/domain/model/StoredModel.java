package com.unicauca.muisca.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

/**
 * Modelo (.xmi) que un usuario guarda en su biblioteca para reutilizarlo en varias
 * ejecuciones. Se asocia al metamodelo registrado cuyo nsURI coincide con el de su raíz.
 */
@Entity
@Table(name = "models", uniqueConstraints = @UniqueConstraint(columnNames = { "owner_id", "name" }))
public class StoredModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "metamodel_id", nullable = false)
    private Metamodel metamodel;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @JdbcTypeCode(SqlTypes.LONG32VARBINARY)
    @Column(nullable = false)
    private byte[] content;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected StoredModel() {
    }

    public StoredModel(String name, String description, Metamodel metamodel, String fileName, byte[] content,
            UserEntity owner) {
        this.name = name;
        this.description = description;
        this.metamodel = metamodel;
        this.fileName = fileName;
        this.content = content;
        this.sizeBytes = content.length;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public void describe(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
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

    public Metamodel getMetamodel() {
        return metamodel;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
