package com.unicauca.muisca.domain.dto;

import java.time.LocalDateTime;

public class AtlFileDTO {
    private Long id;
    private String filename;
    private String fileType;
    private LocalDateTime createdAt;
    
    public AtlFileDTO(Long id, String filename, String fileType, LocalDateTime createdAt) {
        this.id = id;
        this.filename = filename;
        this.fileType = fileType;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    
}