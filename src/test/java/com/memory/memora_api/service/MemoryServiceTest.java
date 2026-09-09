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
import org.springframework.data.mongodb.core.MongoTemplate;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private MongoTemplate mongoTemplate;

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
        MemoryResponseDTO response = memoryService.create(requestDTO);

        // Assert (Verificação)
        assertNotNull(response);
        assertEquals("mem-123", response.id());
        assertEquals("user-1", response.userId());
        assertEquals(8, response.importance());
        verify(memoryRepository, times(1)).save(any(Memory.class));
    }

    @Test
    @DisplayName("Deve incrementar accessCount atomicamente e retornar memória ao buscar por ID")
    void shouldFindByIdAndIncrementAccessCountAtomically() {
        // Arrange
        String id = "mem-123";
        Memory memory = Memory.builder()
                .id(id)
                .userId("user-1")
                .content("Conteúdo de teste")
                .accessCount(1)
                .lastAccessedAt(LocalDateTime.now())
                .build();

        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Memory.class)
        )).thenReturn(memory);

        // Act
        MemoryResponseDTO response = memoryService.findById(id);

        // Assert
        assertNotNull(response);
        assertEquals(id, response.id());
        assertEquals(1, response.accessCount());
        verify(mongoTemplate, times(1)).findAndModify(any(), any(), any(), eq(Memory.class));
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException ao buscar ID inexistente")
    void shouldThrowExceptionWhenIdDoesNotExist() {
        // Arrange
        String nonExistingId = "id-fantasma";
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(Memory.class)
        )).thenReturn(null);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> memoryService.findById(nonExistingId));
        verify(mongoTemplate, times(1)).findAndModify(any(), any(), any(), eq(Memory.class));
    }

    @Test
    @DisplayName("Deve ordenar memórias pelo score normalizado balanceando importância, recência e frequência")
    void shouldReturnMemoriesOrderedByRelevance() {
        Memory memoriaAntigaPopular = Memory.builder()
                .id("mem-antiga")
                .userId("user-1")
                .content("Memória antiga muito consultada")
                .importance(3)
                .accessCount(100)
                .createdAt(LocalDateTime.now().minusDays(30))
                .lastAccessedAt(LocalDateTime.now().minusDays(30))
                .build();

        Memory memoriaRecenteImportante = Memory.builder()
                .id("mem-recente")
                .userId("user-1")
                .content("Memória recente e crítica")
                .importance(9)
                .accessCount(2)
                .createdAt(LocalDateTime.now())
                .lastAccessedAt(LocalDateTime.now())
                .build();

        when(memoryRepository.findByUserId("user-1"))
                .thenReturn(List.of(memoriaAntigaPopular, memoriaRecenteImportante));

        List<MemoryResponseDTO> response = memoryService.findRelevantMemories("user-1");

        assertEquals("mem-recente", response.get(0).id());
        assertNotNull(response.get(0).relevanceScore());
        assertEquals("mem-antiga", response.get(1).id());
        assertNotNull(response.get(1).relevanceScore());
        assertTrue(response.get(0).relevanceScore() > response.get(1).relevanceScore());

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