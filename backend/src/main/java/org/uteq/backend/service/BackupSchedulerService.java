package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.uteq.backend.entity.ConfigBackup;
import org.uteq.backend.repository.ConfigBackupRepository;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class BackupSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(BackupSchedulerService.class);

    private final ConfigBackupRepository configRepo;
    private final BackupService          backupService;

    // Corre cada minuto — compara hora actual con las horas configuradas
    @Scheduled(cron = "0 * * * * *")
    public void verificarYEjecutar() {
        try {
            ConfigBackup cfg = configRepo.findFirstByOrderByIdConfigAsc().orElse(null);
            if (cfg == null || !Boolean.TRUE.equals(cfg.getActivo())) return;

            LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
            int num = cfg.getNumEjecuciones() != null ? cfg.getNumEjecuciones() : 1;

            boolean ejecutar = false;

            if (num >= 1 && cfg.getHoraBackup1() != null) {
                ejecutar |= ahora.equals(cfg.getHoraBackup1().withSecond(0).withNano(0));
            }
            if (num >= 2 && cfg.getHoraBackup2() != null) {
                ejecutar |= ahora.equals(cfg.getHoraBackup2().withSecond(0).withNano(0));
            }
            if (num >= 3 && cfg.getHoraBackup3() != null) {
                ejecutar |= ahora.equals(cfg.getHoraBackup3().withSecond(0).withNano(0));
            }

            if (ejecutar) {
                log.info("Iniciando backup automático a las {}", ahora);
                backupService.ejecutarBackupAutomatico();
            }
        } catch (Exception e) {
            log.warn("BackupScheduler: no se pudo verificar config_backup — {}", e.getMessage());
        }
    }
}