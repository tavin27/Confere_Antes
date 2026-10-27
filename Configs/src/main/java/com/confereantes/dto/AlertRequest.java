package com.confereantes.dto;

import com.confereantes.model.enums.ContactChannel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AlertRequest {

    @NotNull(message = "O canal de contato é obrigatório")
    private ContactChannel contactChannel;

    @Size(max = 150, message = "O contato deve ter no máximo 150 caracteres")
    private String contactValue;

    @NotBlank(message = "O conteúdo é obrigatório")
    @Size(max = 5000, message = "O conteúdo deve ter no máximo 5000 caracteres")
    private String content;

    public ContactChannel getContactChannel() {
        return contactChannel;
    }

    public void setContactChannel(ContactChannel contactChannel) {
        this.contactChannel = contactChannel;
    }

    public String getContactValue() {
        return contactValue;
    }

    public void setContactValue(String contactValue) {
        this.contactValue = contactValue;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
