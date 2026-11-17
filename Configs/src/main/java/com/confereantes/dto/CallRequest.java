package com.confereantes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CallRequest {

    @NotBlank(message = "A URL do áudio é obrigatória")
    @Size(max = 2048, message = "A URL do áudio deve ter no máximo 2048 caracteres")
    private String audioUrl;

    @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
    private String phoneNumber;

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
