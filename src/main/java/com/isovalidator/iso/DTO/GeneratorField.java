package com.isovalidator.iso.DTO;

public record GeneratorField(String key, String label, String section, boolean required,
                             String inputType, String placeholder, String schemaPath) {}
