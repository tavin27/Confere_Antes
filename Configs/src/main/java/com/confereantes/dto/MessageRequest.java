
package com.confereantes.dto;

import com.confereantes.model.enums.MessageSender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MessageRequest {

    @NotBlank(message = "O conteúdo da mensagem é obrigatório")
    @Size(max = 10000, message = "A mensagem deve ter no máximo 10000 caracteres")
    private String content;

    @NotNull(message = "O remetente da mensagem é obrigatório")
    private MessageSender sender;

    public MessageRequest() {
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageSender getSender() {
        return sender;
    }

    public void setSender(MessageSender sender) {
        this.sender = sender;
    }
}
