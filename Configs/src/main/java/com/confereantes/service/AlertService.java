
package com.confereantes.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.confereantes.dto.AlertRequest;
import com.confereantes.dto.AlertResponse;
import com.confereantes.model.Alert;
import com.confereantes.model.User;
import com.confereantes.repository.AlertRepository;
import com.confereantes.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    public AlertService(
            AlertRepository alertRepository,
            UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AlertResponse createAlert(AlertRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário não encontrado"));

        Alert alert = new Alert();

        alert.setUser(user);
        alert.setContactChannel(request.getContactChannel());
        alert.setContactValue(request.getContactValue());

        Alert savedAlert = alertRepository.save(alert);

        return new AlertResponse(
                savedAlert.getId(),
                savedAlert.getContactChannel(),
                savedAlert.getContactValue(),
                savedAlert.getOverallRiskScore(),
                savedAlert.getCreatedAt());
    }
}
