package com.datn.datn.dto;

public class VariantSpecificationDTO {

    private Integer definitionId; // spec_definition.id
    private String value;         // VD: "8GB", "iOS 17"

    // Getters & Setters
    public Integer getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Integer definitionId) {
        this.definitionId = definitionId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
