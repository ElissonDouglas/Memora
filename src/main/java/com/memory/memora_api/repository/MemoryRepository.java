package com.memory.memora_api.repository;

import com.memory.memora_api.model.Memory;
import com.memory.memora_api.model.MemoryType;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MemoryRepository extends MongoRepository<Memory, String> {
    List<Memory> findByUserId(String userId);
    List<Memory> findByUserIdAndType(String userId, MemoryType type);
    // Novo método: Filtra no banco memórias com importância <= X e último acesso antes de Y
    List<Memory> findByImportanceLessThanEqualAndLastAccessedAtBefore(Integer importance, LocalDateTime date);
}
