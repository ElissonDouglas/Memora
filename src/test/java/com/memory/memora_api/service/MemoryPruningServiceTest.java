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
    @DisplayName("Deve deletar memórias antigas, de baixa importância e score irrelevante")
    void shouldPruneEligibleMemories() {
        // Memória elegível: importância baixa (2), inativa há 40 dias e score 0.15
        Memory memoriaObsoleta = Memory.builder()
                .id("mem-lixo")
                .importance(2)
                .lastAccessedAt(LocalDateTime.now().minusDays(40))
                .build();

        // Memória protegida: inativa há 40 dias, mas importância alta (8)
        Memory memoriaImportanteAntiga = Memory.builder()
                .id("mem-valiosa")
                .importance(8)
                .lastAccessedAt(LocalDateTime.now().minusDays(40))
                .build();

        when(memoryRepository.findAll()).thenReturn(List.of(memoriaObsoleta, memoriaImportanteAntiga));
        when(memoryService.calculateRelevanceScore(memoriaObsoleta)).thenReturn(0.15);

        // Executa a poda
        pruningService.pruneForgottenMemories();

        // Verifica se apenas a memória obsoleta foi enviada para deleção
        verify(memoryRepository, times(1)).deleteAll(List.of(memoriaObsoleta));
    }
}