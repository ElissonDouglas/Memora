package com.memory.memory_api.service;

import com.memory.memory_api.dto.MemoryRequestDTO;
import com.memory.memory_api.dto.MemoryResponseDTO;
import com.memory.memory_api.exception.ResourceNotFoundException;
import com.memory.memory_api.model.Memory;
import com.memory.memory_api.model.MemoryScore;
import com.memory.memory_api.model.MemoryType;
import com.memory.memory_api.repository.MemoryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryResponseDTO create(MemoryRequestDTO dto) {
        Memory memory = Memory.builder()
                .userId(dto.userId())
                .type(dto.type())
                .content(dto.content())
                .importance(dto.importance())
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

}
