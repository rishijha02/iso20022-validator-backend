package com.isovalidator.iso.DTO;

import java.util.List;

import lombok.Data;

@Data
public class IsoResponseDTO {

    private String version;
    private String namespace;
    private boolean valid;
    private String messageTyp;
    private String message;
    List<ValidationError> errors;




}
