
package com.confereantes.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    public record ChatTurn(String role, String content) {}
    public record ChatAnalysisResult(String reply, Integer riskScore) {}

    private static final Logger logger = LoggerFactory.getLogger(GeminiAiService.class);

    private final String apiKey;
    private final String model;
    private final String chatModel;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiAiService(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.model:gemini-3.8-flash}") String model,
            @Value("${gemini.chat-model:gemini-2.5-flash}") String chatModel,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.chatModel = chatModel;
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
                                Map.of("text", """
                                        Transcreva este audio em portugues do Brasil, identificando separadamente os participantes da conversa.

                                        REGRAS OBRIGATORIAS:
                                        1. Identifique cada voz com um identificador consistente: PESSOA_1, PESSOA_2, PESSOA_3 etc.
                                        2. Coloque cada fala em uma linha, no formato:
                                           PESSOA_1: texto falado
                                           PESSOA_2: texto falado
                                        3. Preserve a ordem cronologica da conversa.
                                        4. Nao misture falas de participantes diferentes.
                                        5. Nao invente palavras ou informacoes que nao estejam audiveis.
                                        6. Quando nao conseguir identificar com seguranca o participante de uma fala, utilize PARTICIPANTE_INCERTO.
                                        7. Quando um trecho estiver inaudivel, indique [TRECHO_INAUDIVEL].
                                        8. Nao classifique participantes como golpista ou vitima.
                                        9. Nao acrescente explicacoes, comentarios ou analises.

                                        Retorne APENAS a transcricao identificada.
                                        Se o audio nao contiver fala inteligivel, retorne VAZIO.
                                        """)))),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "maxOutputTokens", 4096,
                        "responseMimeType", "text/plain"));

        JsonNode response = postGenerateContent(request);
        String text = extractText(response);
        if (text.isBlank()) {
            throw new IllegalStateException("O Gemini retornou transcricao vazia");
        }
        return text;
    }

    public CallRiskResult analyze(String transcript) {
        requireApiKey();

        String systemPrompt = """
                Voce e um classificador de risco de golpes por telefone. Analise somente a transcricao fornecida.
                Nao invente fatos. Uma suspeita nao e prova. Seja conservador e explique sinais usando trechos literais.
                Retorne exclusivamente um objeto JSON valido, sem markdown, com:
                riskScore (inteiro 0-100), riskLevel (LOW|MEDIUM|HIGH|CRITICAL), isScam (boolean),
                confidence (numero 0-1), recommendation (BLOCK_AND_REPORT|BLOCK_NUMBER|REPORT_SCAM|IGNORE|MARK_AS_SAFE),
                signals (lista de objetos com signalType, description, evidenceText).
                Nao recomende bloqueio automatico; a recomendacao e apenas apoio para decisao humana.
                """;

        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", "Transcricao em portugues:\n\n" + transcript)))),
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))),
                "generationConfig", Map.of(
                        "temperature", 0.0,
                        "maxOutputTokens", 1200,
                        "responseMimeType", "application/json"));

        JsonNode response = postGenerateContent(request);
        String content = extractText(response);
        if (content.isBlank()) {
            throw new IllegalStateException("O Gemini retornou resposta de analise vazia");
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
                throw new IllegalArgumentException("confidence precisa ser numerico");
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
            throw new IllegalStateException("A resposta de analise do Gemini esta invalida", exception);
        }
    }

    /**
     * Analisa uma mensagem de texto do chat e retorna uma resposta amigavel
     * com avaliacao de risco e orientacoes de seguranca, incluindo o score estimado.
     */
    public ChatAnalysisResult analyzeChatText(List<ChatTurn> conversation) {
        requireApiKey();
        if (conversation == null || conversation.isEmpty()) {
            throw new IllegalArgumentException("A conversa nao pode estar vazia");
        }

        String systemPrompt = """
                Voce e o assistente do aplicativo Confere Antes, que ajuda pessoas a identificar golpes por telefone, SMS, WhatsApp e e-mail.
                O usuario esta descrevendo uma situacao suspeita. Sua tarefa e:
                1. Avaliar o nivel de risco (baixo, medio, alto ou critico) com base nos sinais presentes.
                2. Explicar de forma clara e empatica POR QUE a situacao parece segura ou perigosa.
                3. Dar orientacoes praticas e acionaveis sobre o que fazer agora.
                4. NUNCA pedir dados pessoais, senhas, tokens ou codigos.
                5. Responder em portugues do Brasil, de forma direta e acessivel (maximo 180 palavras).
                6. Se o relato contiver sinais classicos de golpe (urgencia, pedido de codigo/token/senha, instalacao de app, transferencia financeira), alerte claramente.
                7. Se nao houver sinais de risco, tranquilize mas recomende cautela. Responda tambem a saudacoes e perguntas curtas usando o contexto anterior.
                8. Considere o historico como contexto; responda a ultima mensagem do usuario e nao repita a analise inicial sem necessidade.

                IMPORTANTE: Voce DEVE responder EXCLUSIVAMENTE com um objeto JSON valido (sem markdown, sem fences), contendo:
                - "reply": string com a resposta em texto simples para o usuario
                - "riskScore": inteiro de 0 a 100 representando o nivel de risco estimado (0=seguro, 100=golpe certo)

                Exemplo de resposta valida: {"reply":"Sua mensagem aqui","riskScore":75}
                """;

        List<Map<String, Object>> contents = conversation.stream()
                .filter(turn -> turn != null && turn.content() != null && !turn.content().isBlank())
                .map(turn -> Map.<String, Object>of(
                        "role", "assistant".equals(turn.role()) ? "model" : "user",
                        "parts", List.of(Map.of("text", turn.content()))))
                .toList();
        if (contents.isEmpty()) {
            throw new IllegalArgumentException("A conversa nao contem mensagens validas");
        }

        Map<String, Object> request = Map.of(
                "contents", contents,
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "maxOutputTokens", 800,
                        "responseMimeType", "application/json"));

        JsonNode response = postGenerateContent(chatModel, request);
        String text = extractText(response);
        if (text.isBlank()) {
            return new ChatAnalysisResult("Nao consegui analisar sua mensagem no momento. Tente novamente.", null);
        }

        try {
            JsonNode result = objectMapper.readTree(stripMarkdownFence(text));
            String reply = result.path("reply").asString("").trim();
            Integer riskScore = null;
            JsonNode scoreNode = result.path("riskScore");
            if (scoreNode.isNumber()) {
                int score = scoreNode.asInt(-1);
                if (score >= 0 && score <= 100) {
                    riskScore = score;
                }
            }
            if (reply.isBlank()) {
                reply = text;
            }
            return new ChatAnalysisResult(reply, riskScore);
        } catch (Exception exception) {
            logger.warn("Failed to parse chat analysis JSON, returning raw text: {}", exception.getMessage());
            return new ChatAnalysisResult(text, null);
        }
    }

    public void validateConfiguration() {
        requireApiKey();
    }

    private JsonNode postGenerateContent(Map<String, Object> request) {
        return postGenerateContent(model, request);
    }

    private JsonNode postGenerateContent(String selectedModel, Map<String, Object> request) {
        try {
            String uri = "/models/" + selectedModel + ":generateContent";
            JsonNode response = restClient.post()
                    .uri(uri)
                    .header("x-goog-api-key", apiKey)
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
            throw new IllegalStateException("A variavel GEMINI_API_KEY nao esta configurada");
        }
    }

    private String requiredText(JsonNode node, String field) {
        String value = node.path(field).asString("").trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatorio ausente: " + field);
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
