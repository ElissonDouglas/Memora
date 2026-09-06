package com.memory.memora_api.dto;

import com.memory.memora_api.model.Memory;
import com.memory.memora_api.model.MemoryType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MemoryResponseDTO(
        String id,
        String userId,
        String content,
        MemoryType type,
        Integer importance,
        Integer accessCount,
        LocalDateTime createdAt,
        LocalDateTime lastAccessedAt,
        Double relevanceScore
) {
    // Conversor sem score (utilizado em create, findById, update)
    public static MemoryResponseDTO fromEntity(Memory memory) {
        return fromEntity(memory, null);
    }

    // Conversor com score explícito (utilizado em rotas de busca/relevância)
    public static MemoryResponseDTO fromEntity(Memory memory, Double relevanceScore) {
        return new MemoryResponseDTO(
                memory.getId(),
                memory.getUserId(),
                memory.getContent(),
                memory.getType(),
                memory.getImportance(),
                memory.getAccessCount(),
                memory.getCreatedAt(),
                memory.getLastAccessedAt(),
                relevanceScore
        );
    }
}