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
        Memory memoryFound = memoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Memória não encontrada."));

        //Incrementa o contador de acessos e altera o lastAccessed para a data e hora atuais
        memoryFound.setAccessCount(memoryFound.getAccessCount() + 1);
        memoryFound.setLastAccessedAt(LocalDateTime.now());

        //Salva as atualizações no objeto memoryFound
        memoryRepository.save(memoryFound);

        return MemoryResponseDTO.fromEntity(memoryFound);
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

    //Relevância = Importância + Peso de Frequência + Peso de Recência
    public List<MemoryResponseDTO> findRelevantMemories(String userId) {
        return memoryRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparingDouble(this::calculateRelevanceScore).reversed())
                .map(MemoryResponseDTO::fromEntity)
                .toList();
    }

    private double calculateRelevanceScore(Memory memory) {
        long hoursSinceLastAccess = Duration.between(memory.getLastAccessedAt(), LocalDateTime.now()).toHours();

        double recencyWeight;
        if (hoursSinceLastAccess < 24) {
            recencyWeight = 3.0;
        } else if (hoursSinceLastAccess < 168) {
            recencyWeight = 1.5;
        } else {
            recencyWeight = 0.0;
        }

        double frequencyWeight = memory.getAccessCount() * 0.5;

        return memory.getImportance() + frequencyWeight + recencyWeight;
    }

    public List<MemoryResponseDTO> searchSimilarMemories(String userId, String query, double minSimilarity) {
        // 1. Gera o vetor da pergunta do usuário/agente
        List<Double> queryEmbedding = embeddingService.generateEmbedding(query);

        // 2. Busca todas as memórias salvas do usuário no banco
        List<Memory> userMemories = memoryRepository.findByUserId(userId);

        // 3. Calcula o score de todas as memórias válidas e guarda em uma lista provisória
        List<AbstractMap.SimpleEntry<Memory, Double>> scoredMemories = userMemories.stream()
                .filter(memory -> memory.getEmbedding() != null && !memory.getEmbedding().isEmpty())
                .map(memory -> {
                    double similarity = VectorMathUtils.cosineSimilarity(queryEmbedding, memory.getEmbedding());
                    log.info("Memória: '{}' | Score: {}", memory.getContent(), similarity);
                    return new AbstractMap.SimpleEntry<>(memory, similarity);
                })
                .toList();

        // 4. Descobre qual foi a nota máxima (o vencedor absoluto)
        double maxScore = scoredMemories.stream()
                .mapToDouble(AbstractMap.SimpleEntry::getValue)
                .max()
                .orElse(0.0);

        // 5. Define a régua dinâmica: deve ser no mínimo o minSimilarity E estar a no máximo 0.08 do vencedor
        double dynamicThreshold = Math.max(minSimilarity, maxScore - 0.08);
        log.info("--- Score Máximo: {} | Threshold Dinâmico Aplicado: {} ---", maxScore, dynamicThreshold);

        // 6. Filtra com a nova régua, ordena do mais próximo para o mais distante e converte
        return scoredMemories.stream()
                .filter(entry -> entry.getValue() >= dynamicThreshold)
                .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                .map(entry -> MemoryResponseDTO.fromEntity(entry.getKey()))
                .toList();
    }

}
