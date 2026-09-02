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
        LocalDateTime lastAccessedAt
) {
    // Construtor auxiliar para converter de Entidade para DTO de forma limpa
    public static MemoryResponseDTO fromEntity(Memory memory) {
        return new MemoryResponseDTO(
                memory.getId(),
                memory.getUserId(),
                memory.getContent(),
                memory.getType(),
                memory.getImportance(),
                memory.getAccessCount(),
                memory.getCreatedAt(),
                memory.getLastAccessedAt()
        );
    }
}