package com.isovalidator.iso.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isovalidator.iso.DTO.OpenBanking.AccountResponse;
import com.isovalidator.iso.DTO.OpenBanking.ConsentRequest;
import com.isovalidator.iso.DTO.OpenBanking.ConsentResponse;
import com.isovalidator.iso.DTO.OpenBanking.TransactionReponse;
import com.isovalidator.iso.service.OpenBanking.Openbankingservice;

import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/api/open-banking/ais/")
public class OpenBankingController {

    @Autowired 
    Openbankingservice eOpenbankingservice;


    @PostMapping("/consent")
    public ResponseEntity<ConsentResponse> createconsent( @RequestBody  ConsentRequest consentRequest)
    {
        return ResponseEntity.ok(eOpenbankingservice.createconsent(consentRequest));
    } 

    @PostMapping("/{consentId}/authorize")
    public ResponseEntity<ConsentResponse> authoriseconsent(@PathVariable String consentId,  @RequestBody  ConsentRequest consentRequest)
    {
        return ResponseEntity.ok(eOpenbankingservice.authoriseconsent(consentId,consentRequest));
    } 

    @PostMapping("/token")
    public ResponseEntity<ConsentResponse> generateaccesstoken(@PathParam(value = "consentId") String consentId,@RequestBody  ConsentRequest consentRequest)
    {
        return ResponseEntity.ok(eOpenbankingservice.getaccesstoken(consentId,consentRequest));
    } 

    @GetMapping ("/accounts")
    public ResponseEntity<List<AccountResponse>> getaccountinfo(@RequestParam String consentId)
    {
        return ResponseEntity.ok(eOpenbankingservice.getaccount(consentId));
    } 


    @GetMapping("/{accountid}/transactions")
    public ResponseEntity<List<TransactionReponse>> getaccountTransactions(@PathVariable  String accountid,@RequestBody  ConsentRequest consentRequest)
    {
        return ResponseEntity.ok(eOpenbankingservice.getTransactions(accountid,consentRequest));
    } 

}
