package com.confereantes.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.confereantes.model.Message;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;

@Component
public class TranscriptMessageAdapter {

    public List<Message> toContactMessages(List<TranscriptSegment> segments) {

        return segments.stream()
                .filter(segment -> segment.text() != null && !segment.text().isBlank())
                .map(segment -> {
                    Message message = new Message();
                    message.setContent(segment.text());
                    message.setSender(MessageSender.CONTATO);
                    message.setSource(MessageSource.AUTOMATICO);
                    return message;
                })
                .toList();
    }
}
