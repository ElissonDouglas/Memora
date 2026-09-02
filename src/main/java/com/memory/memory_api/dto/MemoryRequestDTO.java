package com.memory.memory_api.dto;

import com.memory.memory_api.model.MemoryType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MemoryRequestDTO(
        @NotBlank(message = "O userId é obrigatório.")
        String userId,

        @NotBlank(message = "O conteúdo não pode estar em branco.")
        String content,

        @NotNull(message = "O tipo da memória é obrigatório.")
        MemoryType type,

        @NotNull(message = "A importância é obrigatória.")
        @Min(value = 1, message = "A importância mínima é 1.")
        @Max(value = 10, message = "A importância máxima é 10.")
        Integer importance
) {
}