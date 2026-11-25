package com.confereantes.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Serviço que comunica com a API do classificador ML (FastAPI/Python).
 * Falhas neste serviço NÃO devem impedir o fluxo principal — retorna null como fallback.
 */
@Service
public class MlClassificationService {

    private static final Logger logger = LoggerFactory.getLogger(MlClassificationService.class);

    private final String baseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public MlClassificationService(
            @Value("${ml.service.url:}") String url,
            @Value("${ml.service.enabled:true}") boolean enabled,
            ObjectMapper objectMapper) {
        this.baseUrl = url;
        this.enabled = enabled && url != null && !url.isBlank();
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();

        if (!this.enabled) {
            logger.warn("Serviço ML desabilitado ou URL não configurada (ml.service.url). Classificação ML retornará null.");
        } else {
            logger.info("Serviço ML configurado: {}", this.baseUrl);
        }
    }

    /**
     * Classifica um texto usando o serviço ML externo.
     * Retorna null se o serviço estiver indisponível, desabilitado ou retornar erro.
     */
    public MlClassificationResult classify(String text) {
        if (!enabled || text == null || text.isBlank()) {
            return null;
        }

        try {
            Map<String, String> request = Map.of("text", text);
            JsonNode response = restClient.post()
                    .uri(baseUrl + "/classify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                logger.warn("Serviço ML retornou resposta vazia");
                return null;
            }

            String label = response.path("label").asString("");
            double probability = response.path("probability").asDouble(0.0);
            String riskLevel = response.path("riskLevel").asString("");
            String modelVersion = response.path("modelVersion").asString("");
            String status = response.path("status").asString("");

            List<String> signals = List.of();
            JsonNode signalsNode = response.path("signals");
            if (signalsNode.isArray()) {
                signals = new java.util.ArrayList<>();
                for (JsonNode s : signalsNode) {
                    String val = s.asString("");
                    if (!val.isEmpty()) {
                        signals.add(val);
                    }
                }
                signals = List.copyOf(signals);
            }

            MlClassificationResult result = new MlClassificationResult(
                    label, probability, riskLevel, signals, modelVersion, status);
            logger.debug("Classificação ML: label={} prob={} risk={} status={}", label, probability, riskLevel, status);
            return result;

        } catch (RestClientException exception) {
            logger.warn("Falha ao chamar serviço ML: {}. Usando fallback.", exception.getMessage());
            return null;
        } catch (Exception exception) {
            logger.error("Erro inesperado na classificação ML: {}", exception.getMessage(), exception);
            return null;
        }
    }

    /**
     * Verifica se o serviço ML está disponível.
     */
    public boolean isAvailable() {
        if (!enabled) return false;
        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl + "/health")
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && "ok".equals(response.path("status").asString(""));
        } catch (Exception exception) {
            return false;
        }
    }
}