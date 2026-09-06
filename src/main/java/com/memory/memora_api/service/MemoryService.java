package com.memory.memora_api.service;

import com.memory.memora_api.dto.MemoryRequestDTO;
import com.memory.memora_api.dto.MemoryResponseDTO;
import com.memory.memora_api.exception.ResourceNotFoundException;
import com.memory.memora_api.model.Memory;
import com.memory.memora_api.model.MemoryType;
import com.memory.memora_api.repository.MemoryRepository;
import com.memory.memora_api.util.VectorMathUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.AbstractMap;

@Service
@Slf4j
@AllArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final EmbeddingService embeddingService;
    private final MongoTemplate mongoTemplate;


    private static final double WEIGHT_IMPORTANCE = 0.40;
    private static final double WEIGHT_RECENCY = 0.35;
    private static final double WEIGHT_FREQUENCY = 0.25;

    // Meia-vida de 72 horas para recência: lambda = ln(2) / 72
    private static final double RECENCY_LAMBDA = Math.log(2.0) / 72.0;
    // Ponto de saturação do logaritmo (50 acessos)
    private static final double LOG_MAX_ACCESS = Math.log(51.0);

    public MemoryResponseDTO create(MemoryRequestDTO dto, EmbeddingService embeddingService) {
        List<Double> embedding = embeddingService.generateEmbedding(dto.content());
        Memory memory = Memory.builder()
                .userId(dto.userId())
                .type(dto.type())
                .content(dto.content())
                .importance(dto.importance())
                .embedding(embedding)
                .build();

        Memory savedMemory = memoryRepository.save(memory);
        return MemoryResponseDTO.fromEntity(savedMemory);
    }

    public MemoryResponseDTO findById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));

        Update update = new Update()
                .inc("accessCount", 1)
                .set("lastAccessedAt", LocalDateTime.now());

        // Configura para retornar o documento já atualizado após o incremento
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

        Memory updatedMemory = mongoTemplate.findAndModify(query, update, options, Memory.class);

        if (updatedMemory == null) {
            throw new ResourceNotFoundException("Memória não encontrada.");
        }

        return MemoryResponseDTO.fromEntity(updatedMemory);
    }

    public List<MemoryResponseDTO> findByUser(String userId, MemoryType type) {
        List<Memory> memoriesFound = new ArrayList<>();
        if (type != null) {
            memoriesFound= memoryRepository.findByUserIdAndType(userId, type);
        } else {
            memoriesFound = memoryRepository.findByUserId(userId);
        }

        return memoriesFound.stream()
                .map(MemoryResponseDTO::fromEntity)
                .toList();
    }

    public MemoryResponseDTO update(String id, MemoryRequestDTO dto) {
        Memory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Memória não encontrada."));

        memory.setContent(dto.content());
        memory.setType(dto.type());
        memory.setImportance(dto.importance());
        log.info("Dados atualizados.");

        Memory updatedMemory = memoryRepository.save(memory);

        return MemoryResponseDTO.fromEntity(updatedMemory);
    }

    public void delete(String id) {
        if (!memoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Memória não encontrada.");
        }

        memoryRepository.deleteById(id);

        log.info("Memória deletada.");
    }

    public List<MemoryResponseDTO> findRelevantMemories(String userId) {
        return memoryRepository.findByUserId(userId)
                .stream()
                .map(memory -> new AbstractMap.SimpleEntry<>(memory, calculateRelevanceScore(memory)))
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(entry -> MemoryResponseDTO.fromEntity(entry.getKey(), entry.getValue()))
                .toList();
    }

    protected double calculateRelevanceScore(Memory memory) {
        // 1. Importância normalizada [0.1, 1.0]
        double importanceScore = (memory.getImportance() != null ? memory.getImportance() : 1) / 10.0;

        // 2. Recência contínua com decaimento exponencial [0.0, 1.0]
        LocalDateTime lastAccessed = memory.getLastAccessedAt() != null
                ? memory.getLastAccessedAt()
                : memory.getCreatedAt();
        long hoursElapsed = Duration.between(lastAccessed, LocalDateTime.now()).toHours();
        double recencyScore = Math.exp(-RECENCY_LAMBDA * Math.max(0, hoursElapsed));

        // 3. Frequência sublinear amortecida [0.0, 1.0]
        int count = memory.getAccessCount() != null ? memory.getAccessCount() : 0;
        double frequencyScore = Math.min(1.0, Math.log(count + 1.0) / LOG_MAX_ACCESS);

        return (WEIGHT_IMPORTANCE * importanceScore)
                + (WEIGHT_RECENCY * recencyScore)
                + (WEIGHT_FREQUENCY * frequencyScore);
    }

    public List<MemoryResponseDTO> searchSimilarMemories(String userId, String query, double minSimilarity) {
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);
        List<Memory> userMemories = memoryRepository.findByUserId(userId);

        List<AbstractMap.SimpleEntry<Memory, Double>> scoredMemories = userMemories.stream()
                .filter(memory -> memory.getEmbedding() != null && !memory.getEmbedding().isEmpty())
                .map(memory -> {
                    double similarity = VectorMathUtils.cosineSimilarity(queryEmbedding, memory.getEmbedding());
                    log.info("Memória: '{}' | Score: {}", memory.getContent(), similarity);
                    return new AbstractMap.SimpleEntry<>(memory, similarity);
                })
                .toList();

        double maxScore = scoredMemories.stream()
                .mapToDouble(AbstractMap.SimpleEntry::getValue)
                .max()
                .orElse(0.0);

        double dynamicThreshold = Math.max(minSimilarity, maxScore - 0.08);
        log.info("--- Score Máximo: {} | Threshold Dinâmico Aplicado: {} ---", maxScore, dynamicThreshold);

        return scoredMemories.stream()
                .filter(entry -> entry.getValue() >= dynamicThreshold)
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(entry -> MemoryResponseDTO.fromEntity(entry.getKey(), entry.getValue()))
                .toList();
    }

}
