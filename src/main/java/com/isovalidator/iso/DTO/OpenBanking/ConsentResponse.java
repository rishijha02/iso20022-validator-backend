package com.isovalidator.iso.DTO.OpenBanking;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsentResponse {

    private String consentId;
    List<PermissionDTO> permission;
    private ConsentStatus status; 
    private String access_token;
    private String tokenType;
    private BigDecimal expiresin;

    private String message;



}
