package com.isovalidator.iso.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.isovalidator.iso.DTO.IsoResponseDTO;
import com.isovalidator.iso.DTO.MessageSummaryResponse;
//import com.isovalidator.iso.DTO.IsorequestDTO;
import com.isovalidator.iso.service.IsoValidateService;
import com.isovalidator.iso.service.MessageSummaryServiceImpl;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class IsoController {

    @Autowired
    IsoValidateService isoValidateService;

    @Autowired
    MessageSummaryServiceImpl messageSummaryServiceImpl;


   @PostMapping(
    value = "/v1/api/xmlvalidate",
    consumes = MediaType.APPLICATION_XML_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
)
    public ResponseEntity<IsoResponseDTO> ValidateIsoMsg(@RequestBody String  xml,@RequestParam(defaultValue = "ISO20022") String profile)
    {
        log.info("Entering Controller Validate request for Xml {}");

        IsoResponseDTO responseDTO= isoValidateService.validateMsg(xml,profile);

        return ResponseEntity.ok().body(responseDTO);
    }



    @PostMapping(
        value = "/v1/api/message-summary",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
)
public ResponseEntity<MessageSummaryResponse> getMessageSummary(
        @RequestBody String xml) {

    MessageSummaryResponse response =
            messageSummaryServiceImpl.generateSummary(xml);

    return ResponseEntity.ok(response);
}

@PostMapping(
        value = "/v1/api/xmlvalidate/custom",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
)
public ResponseEntity<IsoResponseDTO> validateCustomXsd(
        @RequestParam("xml") String xml,
        @RequestParam("xsd") MultipartFile xsdFile
) {

    
    IsoResponseDTO responseDTO= isoValidateService.validateWithCustomXsd(
            xml,
            xsdFile);
            return ResponseEntity.ok().body(responseDTO);
}

}
