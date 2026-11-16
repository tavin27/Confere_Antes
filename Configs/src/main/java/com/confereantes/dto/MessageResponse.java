
package com.confereantes.dto;

import java.time.LocalDateTime;

import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;

public class MessageResponse {

    private Long id;
    private String content;
    private MessageSender sender;
    private MessageSource source;
    private LocalDateTime createdAt;

    public MessageResponse() {
    }

    public MessageResponse(
            Long id,
            String content,
            MessageSender sender,
            MessageSource source,
            LocalDateTime createdAt) {

        this.id = id;
        this.content = content;
        this.sender = sender;
        this.source = source;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public MessageSender getSender() {
        return sender;
    }

    public MessageSource getSource() {
        return source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
