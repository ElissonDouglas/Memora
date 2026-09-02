package com.memory.memory_api.controller;

import com.memory.memory_api.dto.MemoryRequestDTO;
import com.memory.memory_api.dto.MemoryResponseDTO;
import com.memory.memory_api.model.MemoryType;
import com.memory.memory_api.service.MemoryService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
@AllArgsConstructor
public class MemoryController {

    private final MemoryService memoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemoryResponseDTO create(@RequestBody @Valid MemoryRequestDTO memoryRequestDTO) {
        return memoryService.create(memoryRequestDTO);
    }

    @GetMapping("/{id}")
    public MemoryResponseDTO getMemory(@PathVariable String id) {
        return memoryService.findById(id);
    }

    @GetMapping("/user/{userId}")
    public List<MemoryResponseDTO> getByUserId(@PathVariable String userId, @RequestParam(required = false)MemoryType type) {
        return memoryService.findByUser(userId, type);
    }

    @PutMapping("/{id}")
    public MemoryResponseDTO updateMemory(@PathVariable String id, @RequestBody @Valid MemoryRequestDTO memoryRequestDTO) {
        return memoryService.update(id, memoryRequestDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable String id) {
        memoryService.delete(id);
    }

    @GetMapping("/{userId}/relevant")
    public List<MemoryResponseDTO> getRelevantMemories(@PathVariable String userId) {
        return memoryService.findRelevantMemories(userId);
    }
}
