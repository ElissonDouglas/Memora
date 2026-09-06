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

        // Busca todas as memórias salvas no MongoDB
        List<Memory> allMemories = memoryRepository.findAll();

        List<Memory> memoriesToDelete = allMemories.stream()
                // 1. Apenas memórias de baixa importância
                .filter(m -> m.getImportance() != null && m.getImportance() <= 3)
                // 2. Não acessadas há pelo menos 30 dias
                .filter(m -> {
                    LocalDateTime lastAccess = m.getLastAccessedAt() != null ? m.getLastAccessedAt() : m.getCreatedAt();
                    return lastAccess != null && lastAccess.isBefore(thresholdDate);
                })
                // 3. Score de relevância geral atual menor que 0.25
                .filter(m -> memoryService.calculateRelevanceScore(m) < 0.25)
                .toList();

        if (!memoriesToDelete.isEmpty()) {
            memoryRepository.deleteAll(memoriesToDelete);
            log.info("Rotina concluída: {} memórias obsoletas foram podadas do banco.", memoriesToDelete.size());
        } else {
            log.info("Rotina concluída: nenhuma memória elegível para poda.");
        }
    }
}