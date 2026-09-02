package com.memory.memora_api.repository;

import com.memory.memora_api.model.Memory;
import com.memory.memora_api.model.MemoryType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MemoryRepository extends MongoRepository<Memory, String> {
    List<Memory> findByUserId(String userId);
    List<Memory> findByUserIdAndType(String userId, MemoryType type);
}
