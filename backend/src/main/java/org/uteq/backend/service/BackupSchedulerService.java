package org.uteq.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.ConfigBackupDTO;

import java.time.LocalTime;
import java.util.concurrent.*;

/**
 * Scheduler dinámico — solo se activa cuando el admin guarda la config con activo=true.
 * No hace ninguna consulta a la BD por su cuenta. Cero llamadas a config_backup
 * desde otros roles.
 */
@Service
public class BackupSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(BackupSchedulerService.class);

    private final BackupService backupService;

    // Pool de 1 hilo para el scheduler dinámico
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> tareaActiva = null;

    // Config en memoria — se actualiza cuando el admin guarda
    private ConfigBackupDTO configActual = null;

    public BackupSchedulerService(BackupService backupService) {
        this.backupService = backupService;
    }

    /**
     * Llamado desde BackupService.guardarConfig() después de guardar en BD.
     * Reinicia el scheduler con la nueva config.
     */
    public void reconfigurar(ConfigBackupDTO cfg) {
        this.configActual = cfg;
        detener();

        if (!Boolean.TRUE.equals(cfg.getActivo())) {
            log.info("Backup automático desactivado.");
            return;
        }

        log.info("Backup automático activado — verificando cada minuto.");

        // Tarea que corre cada minuto y compara hora actual con las horas configuradas
        tareaActiva = executor.scheduleAtFixedRate(() -> {
            try {
                verificarYEjecutar();
            } catch (Exception e) {
                log.warn("BackupScheduler error: {}", e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void verificarYEjecutar() {
        if (configActual == null || !Boolean.TRUE.equals(configActual.getActivo())) return;

        LocalTime ahora = LocalTime.now().withSecond(0).withNano(0);
        int num = configActual.getNumEjecuciones() != null ? configActual.getNumEjecuciones() : 1;

        boolean ejecutar = false;

        if (num >= 1 && configActual.getHoraBackup1() != null && !configActual.getHoraBackup1().isBlank()) {
            ejecutar |= ahora.equals(LocalTime.parse(configActual.getHoraBackup1()).withSecond(0).withNano(0));
        }
        if (num >= 2 && configActual.getHoraBackup2() != null && !configActual.getHoraBackup2().isBlank()) {
            ejecutar |= ahora.equals(LocalTime.parse(configActual.getHoraBackup2()).withSecond(0).withNano(0));
        }
        if (num >= 3 && configActual.getHoraBackup3() != null && !configActual.getHoraBackup3().isBlank()) {
            ejecutar |= ahora.equals(LocalTime.parse(configActual.getHoraBackup3()).withSecond(0).withNano(0));
        }

        if (ejecutar) {
            log.info("Iniciando backup automático a las {}", ahora);
            backupService.ejecutarBackupAutomatico();
        }
    }

    public void detener() {
        if (tareaActiva != null && !tareaActiva.isDone()) {
            tareaActiva.cancel(false);
            tareaActiva = null;
            log.info("Scheduler de backup detenido.");
        }
    }
}