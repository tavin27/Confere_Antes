package com.confereantes.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.confereantes.model.enums.RiskLevel;
import com.confereantes.model.enums.RiskRecommendation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiAiService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAiService.class);

    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiAiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
        this.objectMapper = objectMapper;
    }

    public String transcribe(AudioFile audio) {
        requireApiKey();

        Map<String, Object> inlineData = Map.of(
                "mimeType", audioMimeType(audio.format()),
                "data", Base64.getEncoder().encodeToString(audio.content()));

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(
                                Map.of("inlineData", inlineData),
                                Map.of("text", "Transcreva este áudio em português do Brasil. Retorne APENAS o texto transcrito, sem explicações, sem markdown, sem formatação adicional. Se o áudio não contiver fala inteligível, retorne a palavra VAZIO.")))),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "maxOutputTokens", 4096,
                        "responseMimeType", "text/plain"));

        JsonNode response = postGenerateContent(request);
        String text = extractText(response);
        if (text.isBlank()) {
            throw new IllegalStateException("O Gemini retornou transcrição vazia");
        }
        return text;
    }

    public CallRiskResult analyze(String transcript) {
        requireApiKey();

        String systemPrompt = """
                Você é um classificador de risco de golpes por telefone. Analise somente a transcrição fornecida.
                Não invente fatos. Uma suspeita não é prova. Seja conservador e explique sinais usando trechos literais.
                Retorne exclusivamente um objeto JSON válido, sem markdown, com:
                riskScore (inteiro 0-100), riskLevel (LOW|MEDIUM|HIGH|CRITICAL), isScam (boolean),
                confidence (número 0-1), recommendation (BLOCK_AND_REPORT|BLOCK_NUMBER|REPORT_SCAM|IGNORE|MARK_AS_SAFE),
                signals (lista de objetos com signalType, description, evidenceText).
                Não recomende bloqueio automático; a recomendação é apenas apoio para decisão humana.
                """;

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", "Transcrição em português:\n\n" + transcript)))),
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "maxOutputTokens", 1200,
                        "responseMimeType", "application/json"));

        JsonNode response = postGenerateContent(request);
        String content = extractText(response);
        if (content.isBlank()) {
            throw new IllegalStateException("O Gemini retornou resposta de análise vazia");
        }

        try {
            JsonNode result = objectMapper.readTree(stripMarkdownFence(content));
            int score = result.path("riskScore").asInt(-1);
            if (score < 0 || score > 100) {
                throw new IllegalArgumentException("riskScore fora do intervalo 0-100");
            }

            RiskLevel level = RiskLevel.valueOf(requiredText(result, "riskLevel").toUpperCase());
            RiskRecommendation recommendation = RiskRecommendation
                    .valueOf(requiredText(result, "recommendation").toUpperCase());
            JsonNode scamNode = result.path("isScam");
            if (!scamNode.isBoolean()) {
                throw new IllegalArgumentException("isScam precisa ser booleano");
            }
            var confidenceNode = result.path("confidence");
            if (!confidenceNode.isNumber()) {
                throw new IllegalArgumentException("confidence precisa ser numérico");
            }
            var confidence = confidenceNode.decimalValue();
            if (confidence.signum() < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("confidence fora do intervalo 0-1");
            }

            List<CallRiskResult.Signal> signals = new ArrayList<>();
            JsonNode signalNodes = result.path("signals");
            if (!signalNodes.isArray()) {
                throw new IllegalArgumentException("signals precisa ser uma lista");
            }
            for (JsonNode signal : signalNodes) {
                signals.add(new CallRiskResult.Signal(
                        requiredText(signal, "signalType"),
                        requiredText(signal, "description"),
                        signal.path("evidenceText").asString("")));
            }

            return new CallRiskResult(
                    score,
                    level,
                    scamNode.asBoolean(),
                    confidence,
                    recommendation,
                    List.copyOf(signals),
                    objectMapper.writeValueAsString(result));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("A resposta de análise do Gemini está inválida", exception);
        }
    }

    public void validateConfiguration() {
        requireApiKey();
    }

    private JsonNode postGenerateContent(Map<String, Object> request) {
        try {
            String uri = "/models/" + model + ":generateContent?key=" + apiKey;
            JsonNode response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new IllegalStateException("Gemini retornou resposta vazia");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Gemini retornou HTTP " + exception.getStatusCode().value() + ": " + exception.getResponseBodyAsString(),
                    exception);
        }
    }

    private String extractText(JsonNode response) {
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return "";
        }
        JsonNode parts = candidates.path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asString("");
            if (!text.isEmpty()) {
                sb.append(text);
            }
        }
        return sb.toString().trim();
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A variável GEMINI_API_KEY não está configurada");
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asString("").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente: " + field);
        }
        return value;
    }

    private String stripMarkdownFence(String content) {
        if (!content.startsWith("```")) return content;
        int firstLineEnd = content.indexOf('\n');
        int closingFence = content.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) return content;
        return content.substring(firstLineEnd + 1, closingFence).trim();
    }

    private String audioMimeType(String format) {
        if (format == null) return "audio/mpeg";
        return switch (format.toLowerCase()) {
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "flac" -> "audio/flac";
            case "webm" -> "audio/webm";
            case "mp4", "m4a" -> "audio/mp4";
            default -> "audio/" + format;
        };
    }
}