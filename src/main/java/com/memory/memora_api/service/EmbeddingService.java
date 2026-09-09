package com.memory.memora_api.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmbeddingService {

    private final RestClient restClient;
    private final String apiKey;

    public EmbeddingService(@Value("${gemini.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
    }

    public List<Double> generateEmbedding(String text) {

        try {
            if (text == null || text.isBlank()) {
                return List.of();
            }

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=" + apiKey;

            // O nome do modelo no payload também precisa bater exatamente com a URL
            Map<String, Object> requestBody = Map.of(
                    "model", "models/gemini-embedding-001",
                    "content", Map.of("parts", List.of(Map.of("text", text)))
            );

            Map response = restClient.post()
                    .uri(url)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("embedding")) {
                Map<String, Object> embeddingNode = (Map<String, Object>) response.get("embedding");
                return (List<Double>) embeddingNode.get("values");
            }
            return List.of();
        }
        catch (Exception e) {
            log.error("Erro ao comunicar com a API do Gemini: {}", e.getMessage());
            // Lança uma exceção que pode ser tratada pelo seu GlobalExceptionHandler
            throw new RuntimeException("Falha ao gerar embedding vetorial. Tente novamente mais tarde.", e);
        }

    }
}