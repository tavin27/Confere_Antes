package com.confereantes.service;

public record TranscriptSegment(
        String speakerId,
        String text
) {

    public boolean hasUncertainSpeaker() {
        return speakerId == null
                || speakerId.isBlank()
                || "PARTICIPANTE_INCERTO".equalsIgnoreCase(speakerId.trim());
    }
}
