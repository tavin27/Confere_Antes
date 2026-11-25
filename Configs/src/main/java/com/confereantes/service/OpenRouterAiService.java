package com.confereantes.service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import com.confereantes.model.enums.RiskLevel;
import com.confereantes.model.enums.RiskRecommendation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class OpenRouterAiService {

    public record ChatTurn(String role, String content) {}

    private final String apiKey;
    private final String transcriptionModel;
    private final String analysisModel;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenRouterAiService(
            @Value("${openrouter.api-key:}") String apiKey,
            @Value("${openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${openrouter.transcription-model:openai/whisper-1}") String transcriptionModel,
            @Value("${openrouter.analysis-model:inclusionai/ling-3.2-flash:free}") String analysisModel,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.transcriptionModel = transcriptionModel;
        this.analysisModel = analysisModel;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(45));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
    }

    public String transcribe(AudioFile audio) {
        requireApiKey();
        Map<String, Object> audioInput = Map.of(
                "data", Base64.getEncoder().encodeToString(audio.content()),
                "format", audio.format());
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", transcriptionModel);
        request.put("input_audio", audioInput);
        request.put("language", "pt");
        request.put("response_format", "json");

        JsonNode response = post("/audio/transcriptions", request);
        String text = response.path("text").asString("").trim();
        if (text.isBlank()) {
            throw new IllegalStateException("O serviço de transcrição retornou texto vazio");
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
                "model", analysisModel,
                "stream", false,
                "temperature", 0,
                "max_tokens", 1200,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Transcrição em português:\n\n" + transcript)));

        JsonNode response = post("/chat/completions", request);
        String content = response.path("choices").path(0).path("message").path("content").asString("").trim();
        if (content.isBlank()) {
            throw new IllegalStateException("O modelo de análise retornou uma resposta vazia");
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
            if (confidence.signum() < 0 || confidence.compareTo(java.math.BigDecimal.ONE) > 0) {
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
            throw new IllegalStateException("A resposta de análise do modelo está inválida", exception);
        }
    }

    public String analyzeChatText(List<ChatTurn> conversation) {
        requireApiKey();
        String systemPrompt = """
                Voce e o assistente do aplicativo Confere Antes, especializado em ajudar pessoas a identificar golpes por telefone, SMS, WhatsApp e e-mail.
                Responda a ultima mensagem considerando o historico da conversa.
                Seja empatico, direto e use portugues do Brasil, com no maximo 180 palavras.
                Explique os sinais de risco e diga o que a pessoa pode fazer agora.
                Nunca peca nem repita senhas, tokens, codigos de verificacao ou dados bancarios.
                Se houver pedido de urgencia, transferencia, instalacao de aplicativo ou compartilhamento de codigo, alerte claramente.
                Nao afirme que um contato e golpe sem evidencias suficientes; apresente a avaliacao como orientacao, nao como certeza.
                Responda tambem a mensagens curtas ou cumprimentos, sem ignorar o usuario.
                Retorne apenas texto simples, sem markdown.
                """;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatTurn turn : conversation) {
            if (turn == null || turn.content() == null || turn.content().isBlank()) continue;
            String role = "assistant".equals(turn.role()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", turn.content()));
        }
        if (messages.size() == 1) {
            throw new IllegalArgumentException("A conversa nao possui mensagem para analisar");
        }

        Map<String, Object> request = Map.of(
                "model", analysisModel,
                "stream", false,
                "temperature", 0.3,
                "max_tokens", 800,
                "messages", messages);
        JsonNode response = post("/chat/completions", request);
        String content = response.path("choices").path(0).path("message").path("content").asString("").trim();
        if (content.isBlank()) {
            throw new IllegalStateException("OpenRouter retornou uma resposta de texto vazia");
        }
        return content;
    }

    private JsonNode post(String path, Map<String, Object> request) {
        try {
            JsonNode response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new IllegalStateException("OpenRouter retornou uma resposta vazia");
            }
            return response;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "OpenRouter retornou HTTP " + exception.getStatusCode().value()
                            + ": " + exception.getResponseBodyAsString(),
                    exception);
        }
    }

    private void requireApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A variável OPENROUTER_API_KEY não está configurada");
        }
    }

    public void validateConfiguration() {
        requireApiKey();
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
}
