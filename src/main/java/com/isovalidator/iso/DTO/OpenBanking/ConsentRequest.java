package com.isovalidator.iso.DTO.OpenBanking;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data 
public class ConsentRequest {

    private String ibannum;
    private BigDecimal durationindays;
    private boolean approved;

     List<PermissionDTO> permission;

}
