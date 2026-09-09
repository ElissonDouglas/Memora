package com.memory.memora_api.service;

import com.memory.memora_api.model.Memory;
import com.memory.memora_api.repository.MemoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryPruningServiceTest {

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private MemoryService memoryService;

    @InjectMocks
    private MemoryPruningService pruningService;

    @Test
    @DisplayName("Deve deletar apenas memórias filtradas pelo banco que possuam score irrelevante")
    void shouldPruneEligibleMemories() {
        Memory memoriaObsoleta = Memory.builder()
                .id("mem-lixo")
                .importance(2)
                .lastAccessedAt(LocalDateTime.now().minusDays(40))
                .build();

        Memory memoriaFalsoPositivo = Memory.builder()
                .id("mem-protegida-por-score")
                .importance(3)
                .lastAccessedAt(LocalDateTime.now().minusDays(35))
                .build();

        when(memoryRepository.findByImportanceLessThanEqualAndLastAccessedAtBefore(eq(3), any(LocalDateTime.class)))
                .thenReturn(List.of(memoriaObsoleta, memoriaFalsoPositivo));

        when(memoryService.calculateRelevanceScore(memoriaObsoleta)).thenReturn(0.15);
        when(memoryService.calculateRelevanceScore(memoriaFalsoPositivo)).thenReturn(0.40);

        pruningService.pruneForgottenMemories();

        verify(memoryRepository, times(1)).deleteAll(List.of(memoriaObsoleta));
    }
}