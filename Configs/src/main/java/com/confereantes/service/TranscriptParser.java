
package com.confereantes.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.confereantes.rules.TextNormalizer;

@Component
public class TranscriptParser {

    private static final Pattern SPEAKER_PATTERN = Pattern.compile(
            "^\\s*(PESSOA_\\d+|PARTICIPANTE_INCERTO)\\s*:\\s*(.*)$",
            Pattern.CASE_INSENSITIVE
    );

    public List<TranscriptSegment> parse(String transcript) {

        if (transcript == null || transcript.isBlank()) {
            return List.of();
        }

        List<TranscriptSegment> segments = new ArrayList<>();

        for (String line : transcript.split("\\R")) {

            if (line.isBlank()) {
                continue;
            }

            Matcher matcher = SPEAKER_PATTERN.matcher(line);

            if (!matcher.matches()) {
                segments.add(new TranscriptSegment(
                        "PARTICIPANTE_INCERTO",
                        line.trim()
                ));
                continue;
            }

            String speakerId = matcher.group(1).toUpperCase();
            String text = matcher.group(2).trim();

            if (text.isBlank()) {
                continue;
            }

            if (TextNormalizer.normalize(text).equals("[trecho_inaudivel]")) {
                continue;
            }

            segments.add(new TranscriptSegment(speakerId, text));
        }

        return List.copyOf(segments);
    }
}

