package com.confereantes.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.confereantes.dto.CallRequest;
import com.confereantes.dto.CallResponse;
import com.confereantes.dto.UserDecisionRequest;
import com.confereantes.service.CallAnalysisService;

import jakarta.validation.Valid;

@RestController
@RequestMapping
public class CallAnalysisController {

    private final CallAnalysisService callAnalysisService;

    public CallAnalysisController(CallAnalysisService callAnalysisService) {
        this.callAnalysisService = callAnalysisService;
    }

    @PostMapping("/alerts/{alertId}/calls")
    public ResponseEntity<CallResponse> createCall(
            @PathVariable Long alertId,
            @Valid @RequestBody CallRequest request,
            Authentication authentication) {

        CallResponse response = callAnalysisService.createCall(
                alertId,
                authentication.getName(),
                request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/calls/{callId}")
    public ResponseEntity<CallResponse> getCall(
            @PathVariable Long callId,
            Authentication authentication) {

        return ResponseEntity.ok(callAnalysisService.getCall(
                callId,
                authentication.getName()));
    }

    @PostMapping("/calls/{callId}/decision")
    public ResponseEntity<CallResponse> registerDecision(
            @PathVariable Long callId,
            @Valid @RequestBody UserDecisionRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(callAnalysisService.registerDecision(
                callId,
                authentication.getName(),
                request));
    }
}
