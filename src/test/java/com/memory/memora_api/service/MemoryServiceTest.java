package com.memory.memora_api.service;

import com.memory.memora_api.dto.MemoryRequestDTO;
import com.memory.memora_api.dto.MemoryResponseDTO;
import com.memory.memora_api.exception.ResourceNotFoundException;
import com.memory.memora_api.model.Memory;
import com.memory.memora_api.model.MemoryType;
import com.memory.memora_api.repository.MemoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private MemoryRepository memoryRepository;

    @InjectMocks
    private MemoryService memoryService;

    @Test
    @DisplayName("Deve criar uma memória com sucesso")
    void shouldCreateMemorySuccessfully() {
        // Arrange (Preparação)
        MemoryRequestDTO requestDTO = new MemoryRequestDTO(
                "user-1",
                "Gosta de explicações práticas",
                MemoryType.PREFERENCE,
                8
        );

        Memory savedMemory = Memory.builder()
                .id("mem-123")
                .userId(requestDTO.userId())
                .content(requestDTO.content())
                .type(requestDTO.type())
                .importance(requestDTO.importance())
                .accessCount(0)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        when(embeddingService.generateEmbedding(any(String.class))).thenReturn(List.of(0.1, 0.2, 0.3));
        when(memoryRepository.save(any(Memory.class))).thenReturn(savedMemory);

        // Act (Ação)
        MemoryResponseDTO response = memoryService.create(requestDTO, embeddingService);

        // Assert (Verificação)
        assertNotNull(response);
        assertEquals("mem-123", response.id());
        assertEquals("user-1", response.userId());
        assertEquals(8, response.importance());
        verify(memoryRepository, times(1)).save(any(Memory.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ID inexistente")
    void shouldThrowExceptionWhenIdDoesNotExist() {
        // Arrange
        String nonExistingId = "id-fantasma";
        when(memoryRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> memoryService.findById(nonExistingId));
        verify(memoryRepository, times(1)).findById(nonExistingId);
    }

    @Test
    @DisplayName("Deve retornar memórias ordenadas por relevância de forma decrescente")
    void shouldReturnMemoriesOrderedByRelevance() {
        //Arrange
        Memory memoriaBaixa = Memory.builder()
                .id("mem-baixa")
                .userId("user-1")
                .content("Essa é uma memória baixa")
                .type(MemoryType.FACT)
                .importance(3)
                .accessCount(0)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();


        Memory memoriaAlta = Memory.builder()
                .id("mem-alta")
                .userId("user-1")
                .content("Essa é uma memória alta")
                .type(MemoryType.FACT)
                .importance(9)
                .accessCount(10)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        when(memoryRepository.findByUserId("user-1")).thenReturn(List.of(memoriaBaixa, memoriaAlta));


        // Act
        List<MemoryResponseDTO> response = memoryService.findRelevantMemories("user-1");
        assertEquals("mem-alta", response.get(0).id());
        assertEquals("mem-baixa", response.get(1).id());
        verify(memoryRepository, times(1)).findByUserId("user-1");
    }

    @Test
    @DisplayName("Deve realizar busca semântica e filtrar pelo threshold de similaridade")
    void shouldReturnSimilarMemoriesBasedOnCosineSimilarity() {
        // Arrange
        String userId = "user-1";
        String query = "Assunto de teste";

        // vetor da pergunta
        List<Double> queryVector = List.of(1.0, 0.0, 0.0);

        // Memória 1: Vetor idêntico à pergunta (Cosseno = 1.0)
        Memory memoriaRelacionada = Memory.builder()
                .id("mem-relacionada")
                .userId(userId)
                .content("Conteúdo que importa")
                .embedding(List.of(1.0, 0.0, 0.0))
                .build();

        // Memória 2: Vetor perpendicular/totalmente diferente (Cosseno = 0.0)
        Memory memoriaIrrelevante = Memory.builder()
                .id("mem-irrelevante")
                .userId(userId)
                .content("Conteúdo aleatório")
                .embedding(List.of(0.0, 1.0, 0.0))
                .build();

        when(embeddingService.generateEmbedding(query)).thenReturn(queryVector);
        when(memoryRepository.findByUserId(userId)).thenReturn(List.of(memoriaIrrelevante, memoriaRelacionada));

        // Act - Definimos a similaridade mínima em 0.5
        List<MemoryResponseDTO> results = memoryService.searchSimilarMemories(userId, query, 0.5);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size(), "Deve retornar apenas a memória que passou na linha de corte");
        assertEquals("mem-relacionada", results.get(0).id(), "A memória retornada deve ser a relacionada");

        verify(embeddingService, times(1)).generateEmbedding(query);
        verify(memoryRepository, times(1)).findByUserId(userId);
    }
}