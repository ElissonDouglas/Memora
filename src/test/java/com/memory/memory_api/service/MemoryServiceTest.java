package com.memory.memory_api.service;

import com.memory.memory_api.dto.MemoryRequestDTO;
import com.memory.memory_api.dto.MemoryResponseDTO;
import com.memory.memory_api.exception.ResourceNotFoundException;
import com.memory.memory_api.model.Memory;
import com.memory.memory_api.model.MemoryType;
import com.memory.memory_api.repository.MemoryRepository;
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
}