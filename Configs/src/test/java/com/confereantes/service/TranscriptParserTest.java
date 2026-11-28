
package com.confereantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class TranscriptParserTest {

    private final TranscriptParser parser = new TranscriptParser();

    @Test
    void shouldParseIdentifiedParticipants() {

        String transcript = """
                PESSOA_1: Bom dia, estou ligando do banco.
                PESSOA_2: Qual o motivo da ligacao?
                PESSOA_1: Precisamos confirmar sua senha.
                """;

        List<TranscriptSegment> segments = parser.parse(transcript);

        assertEquals(3, segments.size());
        assertEquals("PESSOA_1", segments.get(0).speakerId());
        assertEquals("Bom dia, estou ligando do banco.", segments.get(0).text());
        assertEquals("PESSOA_2", segments.get(1).speakerId());
        assertEquals("PESSOA_1", segments.get(2).speakerId());
    }

    @Test
    void shouldMarkUnidentifiedParticipantAsUncertain() {

        String transcript = """
                PARTICIPANTE_INCERTO: Nao foi possivel identificar a voz.
                """;

        List<TranscriptSegment> segments = parser.parse(transcript);

        assertEquals(1, segments.size());
        assertTrue(segments.get(0).hasUncertainSpeaker());
    }

    @Test
    void shouldMarkMalformedLineAsUncertain() {

        String transcript = """
                Bom dia, estou ligando do banco.
                """;

        List<TranscriptSegment> segments = parser.parse(transcript);

        assertEquals(1, segments.size());
        assertTrue(segments.get(0).hasUncertainSpeaker());
    }

    @Test
    void shouldIgnoreEmptyLinesAndEmptySegments() {

        String transcript = """
                PESSOA_1: Bom dia.

                PESSOA_2:
                PESSOA_1: Tudo bem?
                """;

        List<TranscriptSegment> segments = parser.parse(transcript);

        assertEquals(2, segments.size());
    }

    @Test
    void shouldReturnEmptyListForBlankTranscript() {

        List<TranscriptSegment> segments = parser.parse(" ");

        assertTrue(segments.isEmpty());
    }

    @Test
    void shouldIgnoreUnaudibleSegment() {

        String transcript = """
                PESSOA_1: Bom dia.
                PESSOA_2: [TRECHO_INAUDIVEL]
                """;

        List<TranscriptSegment> segments = parser.parse(transcript);

        assertEquals(1, segments.size());
        assertEquals("PESSOA_1", segments.get(0).speakerId());
    }
}

