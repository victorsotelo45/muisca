package com.unicauca.muisca.domain.dto;

public class AtlTransformRequest {
    private String transformationAsmPath;
    private String inputModelPath;
    private String inputMetamodelPath;
    private String outputMetamodelPath;
    private String outputModelPath;
    private String inputModelName;
    private String outputModelName;

    // Getters y setters
    public String getTransformationAsmPath() {
        return transformationAsmPath;
    }

    public void setTransformationAsmPath(String transformationAsmPath) {
        this.transformationAsmPath = transformationAsmPath;
    }

    public String getInputModelPath() {
        return inputModelPath;
    }

    public void setInputModelPath(String inputModelPath) {
        this.inputModelPath = inputModelPath;
    }

    public String getInputMetamodelPath() {
        return inputMetamodelPath;
    }

    public void setInputMetamodelPath(String inputMetamodelPath) {
        this.inputMetamodelPath = inputMetamodelPath;
    }

    public String getOutputMetamodelPath() {
        return outputMetamodelPath;
    }

    public void setOutputMetamodelPath(String outputMetamodelPath) {
        this.outputMetamodelPath = outputMetamodelPath;
    }

    public String getOutputModelPath() {
        return outputModelPath;
    }

    public void setOutputModelPath(String outputModelPath) {
        this.outputModelPath = outputModelPath;
    }

    public String getInputModelName() {
        return inputModelName;
    }

    public void setInputModelName(String inputModelName) {
        this.inputModelName = inputModelName;
    }

    public String getOutputModelName() {
        return outputModelName;
    }

    public void setOutputModelName(String outputModelName) {
        this.outputModelName = outputModelName;
    }
}
