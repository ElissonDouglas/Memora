package com.memory.memora_api.service;

import com.memory.memora_api.model.Memory;
import com.memory.memora_api.repository.MemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryPruningService {

    private final MemoryRepository memoryRepository;
    private final MemoryService memoryService;

    // Executa diariamente às 03:00 da manhã (cron: segundo minuto hora dia mês dia-da-semana)
    @Scheduled(cron = "0 0 3 * * *")
    public void pruneForgottenMemories() {
        log.info("Iniciando rotina agendada de esquecimento de memórias...");

        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(30);

        // O Mongo já filtra a importância e a inatividade. Agora retorna apenas os candidatos reais
        List<Memory> candidateMemories = memoryRepository
                .findByImportanceLessThanEqualAndLastAccessedAtBefore(3, thresholdDate);

        // O Java filtra apenas o cálculo complexo do score
        List<Memory> memoriesToDelete = candidateMemories.stream()
                .filter(m -> memoryService.calculateRelevanceScore(m) < 0.25)
                .toList();

        if (!memoriesToDelete.isEmpty()) {
            memoryRepository.deleteAll(memoriesToDelete);
            log.info("Rotina concluída: {} memórias obsoletas podadas.", memoriesToDelete.size());
        }
    }
}