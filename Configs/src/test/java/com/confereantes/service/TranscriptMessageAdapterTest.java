
package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.confereantes.model.Message;
import com.confereantes.model.enums.MessageSender;
import com.confereantes.model.enums.MessageSource;

class TranscriptMessageAdapterTest {

    private final TranscriptMessageAdapter adapter =
            new TranscriptMessageAdapter();

    @Test
    void shouldConvertIdentifiedSegmentsIntoContactMessages() {

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment("PESSOA_1", "Informe sua senha."),
                new TranscriptSegment("PESSOA_2", "Não vou informar.")
        );

        List<Message> messages = adapter.toContactMessages(segments);

        assertEquals(2, messages.size());
        assertEquals("Informe sua senha.", messages.get(0).getContent());
        assertEquals(MessageSender.CONTATO, messages.get(0).getSender());
        assertEquals(MessageSource.AUTOMATICO, messages.get(0).getSource());
    }

    @Test
    void shouldConvertUncertainSegmentsIntoMessages() {

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment("PESSOA_1", "Informe sua senha."),
                new TranscriptSegment("PARTICIPANTE_INCERTO", "Faça a transferência.")
        );

        List<Message> messages = adapter.toContactMessages(segments);

        assertEquals(2, messages.size());
        assertEquals("Faça a transferência.", messages.get(1).getContent());
        assertEquals(MessageSender.CONTATO, messages.get(1).getSender());
        assertEquals(MessageSource.AUTOMATICO, messages.get(1).getSource());
    }

    @Test
    void shouldConvertAllUncertainSegmentsWhenTheyContainText() {

        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment("PARTICIPANTE_INCERTO", "Informe sua senha.")
        );

        List<Message> messages = adapter.toContactMessages(segments);

        assertFalse(messages.isEmpty());
        assertEquals(1, messages.size());
        assertEquals("Informe sua senha.", messages.get(0).getContent());
    }
}
