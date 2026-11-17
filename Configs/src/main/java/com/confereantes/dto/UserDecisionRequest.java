package com.confereantes.dto;

import com.confereantes.model.enums.UserDecision;

import jakarta.validation.constraints.NotNull;

public class UserDecisionRequest {

    @NotNull(message = "A decisão do usuário é obrigatória")
    private UserDecision decision;

    public UserDecision getDecision() { return decision; }
    public void setDecision(UserDecision decision) { this.decision = decision; }
}
