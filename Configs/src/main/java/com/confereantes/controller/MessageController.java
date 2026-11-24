
package com.confereantes.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.confereantes.dto.MessageRequest;
import com.confereantes.dto.MessageResponse;
import com.confereantes.model.Message;
import com.confereantes.service.MessageService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/alerts/{alertId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> createMessage(
            @PathVariable Long alertId,
            Authentication authentication,
            @Valid @RequestBody MessageRequest request) {

        String userEmail = authentication.getName();

        Message message = messageService.createMessage(
                alertId,
                userEmail,
                request.getContent(),
                request.getSender());

        MessageResponse response = new MessageResponse(
                message.getId(),
                message.getContent(),
                message.getSender(),
                message.getSource(),
                message.getCreatedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            @PathVariable Long alertId,
            Authentication authentication) {

        String userEmail = authentication.getName();

        List<Message> messages = messageService.getMessagesByAlert(
                alertId, userEmail);

        List<MessageResponse> response = messages.stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getContent(),
                        message.getSender(),
                        message.getSource(),
                        message.getCreatedAt()))
                .toList();

        return ResponseEntity.ok(response);
    }
}
