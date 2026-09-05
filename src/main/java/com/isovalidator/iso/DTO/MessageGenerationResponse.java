package com.isovalidator.iso.DTO;

import lombok.Data;

@Data
public class MessageGenerationResponse {

    private boolean success;

    private String messageType;

    private String geography;

    private String version;

    private String xml;

    private String error;

}
