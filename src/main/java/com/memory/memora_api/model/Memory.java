package com.memory.memora_api.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "memories")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Memory {
    @Id
    private String id;
    private String userId;
    private String content;
    private MemoryType type;
    private Integer importance;

    private List<Double> embedding;

    @Builder.Default
    private Integer accessCount = 0;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime lastAccessedAt = LocalDateTime.now();
}