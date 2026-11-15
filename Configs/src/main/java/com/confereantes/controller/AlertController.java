
package com.confereantes.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.confereantes.dto.AlertRequest;
import com.confereantes.dto.AlertResponse;
import com.confereantes.service.AlertService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(
            @Valid @RequestBody AlertRequest request) {

        Long userId = 1L;

        AlertResponse response = alertService.createAlert(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
