package org.uteq.backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.ConfigBackupDTO;
import org.uteq.backend.dto.HistorialBackupDTO;
import org.uteq.backend.entity.ConfigBackup;
import org.uteq.backend.entity.HistorialBackup;
import org.uteq.backend.repository.ConfigBackupRepository;
import org.uteq.backend.repository.HistorialBackupRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final ConfigBackupRepository    configRepo;
    private final HistorialBackupRepository historialRepo;
    private final NotificacionService       notificacionService;
    private final DynamicMailService        dynamicMailService;

    @Lazy
    @Autowired
    private BackupSchedulerService schedulerService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    // =========================================================================
    // INIT SCHEDULER AL ARRANCAR
    // =========================================================================

    @PostConstruct
    public void inicializarScheduler() {
        try {
            ConfigBackup cfg = configRepo.findFirstByOrderByIdConfigAsc().orElse(null);
            if (cfg != null) schedulerService.reconfigurar(toConfigDTO(cfg));
        } catch (Exception e) {
            log.warn("No se pudo inicializar el scheduler de backup: {}", e.getMessage());
        }
    }

    // =========================================================================
    // CONFIG
    // =========================================================================

    public ConfigBackupDTO obtenerConfig() {
        ConfigBackup cfg = configRepo.findFirstByOrderByIdConfigAsc()
                .orElseGet(this::crearConfigDefecto);
        return toConfigDTO(cfg);
    }

    public ConfigBackupDTO guardarConfig(ConfigBackupDTO dto) {
        ConfigBackup cfg = configRepo.findFirstByOrderByIdConfigAsc()
                .orElse(new ConfigBackup());

        cfg.setRutaPgdump(dto.getRutaPgdump() != null && !dto.getRutaPgdump().isBlank()
                ? dto.getRutaPgdump() : "pg_dump");
        cfg.setRutaOrigen(dto.getRutaOrigen());
        cfg.setTipoBackup(dto.getTipoBackup());
        cfg.setRetencionActiva(dto.getRetencionActiva());
        cfg.setDiasRetencion(dto.getDiasRetencion() != null ? dto.getDiasRetencion() : 7);
        cfg.setActivo(dto.getActivo());
        cfg.setNotificarError(dto.getNotificarError());
        cfg.setNotificarExito(dto.getNotificarExito());

        // Destinos múltiples
        cfg.setDestinoLocal(Boolean.TRUE.equals(dto.getDestinoLocal()));
        cfg.setDestinoEmail(Boolean.TRUE.equals(dto.getDestinoEmail()));
        cfg.setRutaDestino(dto.getRutaDestino());
        cfg.setEmailDestino(dto.getEmailDestino());

        // Legacy tipoDestino — derivar del estado actual para compatibilidad
        if (Boolean.TRUE.equals(cfg.getDestinoEmail()) && Boolean.TRUE.equals(cfg.getDestinoLocal())) cfg.setTipoDestino("LOCAL");
        else if (Boolean.TRUE.equals(cfg.getDestinoEmail()))                     cfg.setTipoDestino("EMAIL");
        else if (Boolean.TRUE.equals(cfg.getDestinoLocal()))                     cfg.setTipoDestino("LOCAL");
        else                                               cfg.setTipoDestino("NINGUNO");

        int num = dto.getNumEjecuciones() != null ? dto.getNumEjecuciones() : 1;
        cfg.setNumEjecuciones(num);
        cfg.setHoraBackup1(dto.getHoraBackup1() != null ? LocalTime.parse(dto.getHoraBackup1()) : LocalTime.of(8, 0));
        cfg.setHoraBackup2(num >= 2 && dto.getHoraBackup2() != null && !dto.getHoraBackup2().isBlank()
                ? LocalTime.parse(dto.getHoraBackup2()) : null);
        cfg.setHoraBackup3(num >= 3 && dto.getHoraBackup3() != null && !dto.getHoraBackup3().isBlank()
                ? LocalTime.parse(dto.getHoraBackup3()) : null);

        ConfigBackupDTO resultado = toConfigDTO(configRepo.save(cfg));
        // Notificar al scheduler para que se reconfigure sin tocar la BD
        schedulerService.reconfigurar(resultado);
        return resultado;
    }

    // =========================================================================
    // EJECUTAR
    // =========================================================================

    public HistorialBackupDTO ejecutarBackupManual() {
        return ejecutarBackup(obtenerOCrearConfig(), "MANUAL");
    }

    public HistorialBackupDTO ejecutarBackupAutomatico() {
        return ejecutarBackup(obtenerOCrearConfig(), "AUTOMATICO");
    }

    private HistorialBackupDTO ejecutarBackup(ConfigBackup cfg, String origen) {
        HistorialBackup historial = new HistorialBackup();
        historial.setFechaInicio(LocalDateTime.now());
        historial.setTipoBackup(cfg.getTipoBackup());
        historial.setOrigen(origen);

        long inicio = System.currentTimeMillis();

        try {
            // Crear carpeta local principal si no existe
            crearCarpetaSiNoExiste(cfg.getRutaOrigen());

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            String nombreDump = "backup_" + timestamp + ".dump";
            String rutaDump   = cfg.getRutaOrigen() + File.separator + nombreDump;
            String nombreZip  = "backup_" + timestamp + ".zip";
            String rutaZip    = cfg.getRutaOrigen() + File.separator + nombreZip;

            // 1. pg_dump
            ejecutarPgDump(cfg.getRutaPgdump(), rutaDump);

            // 2. Comprimir
            comprimirArchivo(rutaDump, rutaZip);
            Files.deleteIfExists(Paths.get(rutaDump));

            long tamano   = Files.size(Paths.get(rutaZip));
            long duracion = (System.currentTimeMillis() - inicio) / 1000;

            // 3. Destino LOCAL secundario (pendrive/red)
            if (Boolean.TRUE.equals(cfg.getDestinoLocal())
                    && cfg.getRutaDestino() != null
                    && !cfg.getRutaDestino().isBlank()) {
                crearCarpetaSiNoExiste(cfg.getRutaDestino());
                Files.copy(Paths.get(rutaZip),
                        Paths.get(cfg.getRutaDestino(), nombreZip),
                        StandardCopyOption.REPLACE_EXISTING);
                log.info("Backup copiado a destino local: {}", cfg.getRutaDestino());
            }

            // 4. Destino EMAIL
            if (Boolean.TRUE.equals(cfg.getDestinoEmail())) {
                enviarBackupPorEmail(cfg.getEmailDestino(), rutaZip, nombreZip, tamano);
            }

            historial.setEstado("EXITOSO");
            historial.setRutaArchivo(rutaZip);
            historial.setTamanoBytes(tamano);
            historial.setDuracionSegundos(duracion);
            historial.setFechaFin(LocalDateTime.now());
            historialRepo.save(historial);

            if (Boolean.TRUE.equals(cfg.getRetencionActiva())) {
                limpiarBackupsAntiguos(cfg);
            }

            if (Boolean.TRUE.equals(cfg.getNotificarExito())) {
                try {
                    notificacionService.notificarRol(
                            "admin", "success",
                            "Backup exitoso ✅",
                            "Backup " + cfg.getTipoBackup() + " completado. "
                                    + nombreZip + " (" + formatearTamano(tamano) + ") en " + duracion + "s.",
                            "BACKUP", historial.getIdHistorial()
                    );
                } catch (Exception ne) {
                    log.warn("No se pudo enviar notificacion de exito: {}", ne.getMessage());
                }
            }

            log.info("Backup exitoso: {} en {}s", nombreZip, duracion);

        } catch (Exception e) {
            long duracion = (System.currentTimeMillis() - inicio) / 1000;
            historial.setEstado("FALLIDO");
            historial.setMensajeError(e.getMessage());
            historial.setDuracionSegundos(duracion);
            historial.setFechaFin(LocalDateTime.now());
            historialRepo.save(historial);

            if (Boolean.TRUE.equals(cfg.getNotificarError())) {
                try {
                    notificacionService.notificarRol(
                            "admin", "error",
                            "Error en backup ❌",
                            "El backup falló: " + e.getMessage(),
                            "BACKUP", null
                    );
                } catch (Exception ne) {
                    log.warn("No se pudo enviar notificacion de error: {}", ne.getMessage());
                }
            }
            log.error("Error en backup: {}", e.getMessage());
        }

        return toHistorialDTO(historial);
    }

    // =========================================================================
    // CREAR CARPETA AUTOMÁTICAMENTE
    // =========================================================================

    private void crearCarpetaSiNoExiste(String ruta) throws IOException {
        if (ruta == null || ruta.isBlank()) return;
        Path path = Paths.get(ruta);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Carpeta creada automáticamente: {}", ruta);
        }
    }

    // =========================================================================
    // pg_dump
    // =========================================================================

    private void ejecutarPgDump(String rutaPgdump, String rutaSalida)
            throws IOException, InterruptedException {

        String clean   = datasourceUrl.replace("jdbc:postgresql://", "").split("\\?")[0];
        String[] partes = clean.split("/");
        String dbname   = partes[partes.length - 1];
        String[] hp     = partes[0].split(":");
        String host     = hp[0];
        String puerto   = hp.length > 1 ? hp[1] : "5432";

        String pgDumpExe = (rutaPgdump != null && !rutaPgdump.isBlank()) ? rutaPgdump : "pg_dump";

        ProcessBuilder pb = new ProcessBuilder(
                pgDumpExe,
                "-h", host,
                "-p", puerto,
                "-U", datasourceUsername,
                "--no-owner",
                "--no-acl",
                "--schema=public",
                "-F", "c",
                "-f", rutaSalida,
                dbname
        );
        pb.environment().put("PGPASSWORD", datasourcePassword);
        pb.redirectErrorStream(true);

        Process proceso  = pb.start();
        String  output   = new String(proceso.getInputStream().readAllBytes());
        int     exitCode = proceso.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                    "pg_dump falló (código " + exitCode + "): " + output);
        }
    }

    // =========================================================================
    // EMAIL
    // =========================================================================

    private void enviarBackupPorEmail(String destino, String rutaZip,
                                      String nombreZip, long tamano) throws Exception {
        if (destino == null || destino.isBlank()) {
            throw new RuntimeException("No se configuró email de destino para el backup.");
        }

        String[] destinatarios = destino.split("[,;]");
        List<String> emails = Arrays.stream(destinatarios)
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toList());

        if (emails.isEmpty()) {
            throw new RuntimeException("No se encontró ningún email de destino válido.");
        }

        MimeMessage message = dynamicMailService.getMailSender().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(dynamicMailService.getEmailFrom());
        helper.setTo(emails.toArray(new String[0]));
        helper.setSubject("Backup BD — " + nombreZip);
        helper.setText(
                "<h3>Backup de base de datos</h3>" +
                        "<p>Se adjunta el backup generado automáticamente.</p>" +
                        "<p><b>Archivo:</b> " + nombreZip + "<br>" +
                        "<b>Tamaño:</b> " + formatearTamano(tamano) + "<br>" +
                        "<b>Fecha:</b> " + LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "</p>",
                true
        );

        FileSystemResource file = new FileSystemResource(new File(rutaZip));
        helper.addAttachment(nombreZip, file);

        dynamicMailService.getMailSender().send(message);
        log.info("Backup enviado por email a {}", String.join(", ", emails));
    }

    // =========================================================================
    // ZIP
    // =========================================================================

    private void comprimirArchivo(String rutaArchivo, String rutaZip) throws IOException {
        Path archivo = Paths.get(rutaArchivo);
        try (ZipOutputStream zos = new ZipOutputStream(
                Files.newOutputStream(Paths.get(rutaZip)))) {
            zos.putNextEntry(new ZipEntry(archivo.getFileName().toString()));
            Files.copy(archivo, zos);
            zos.closeEntry();
        }
    }

    // =========================================================================
    // LIMPIEZA
    // =========================================================================

    private void limpiarBackupsAntiguos(ConfigBackup cfg) {
        try {
            File carpeta = new File(cfg.getRutaOrigen());
            File[] zips  = carpeta.listFiles((d, n) -> n.endsWith(".zip"));
            if (zips == null) return;
            LocalDateTime limite = LocalDateTime.now().minusDays(cfg.getDiasRetencion());
            for (File f : zips) {
                LocalDateTime fecha = LocalDateTime.ofInstant(
                        java.nio.file.attribute.FileTime
                                .fromMillis(f.lastModified()).toInstant(),
                        ZoneId.systemDefault());
                if (fecha.isBefore(limite)) {
                    f.delete();
                    log.info("Backup antiguo eliminado: {}", f.getName());
                }
            }
        } catch (Exception e) {
            log.warn("Error al limpiar backups antiguos: {}", e.getMessage());
        }
    }

    // =========================================================================
    // HISTORIAL
    // =========================================================================

    public List<HistorialBackupDTO> obtenerHistorial() {
        return historialRepo.findTop50ByOrderByFechaInicioDesc()
                .stream().map(this::toHistorialDTO).collect(Collectors.toList());
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private ConfigBackup obtenerOCrearConfig() {
        return configRepo.findFirstByOrderByIdConfigAsc()
                .orElseGet(this::crearConfigDefecto);
    }

    private ConfigBackup crearConfigDefecto() {
        ConfigBackup cfg = new ConfigBackup();
        cfg.setRutaPgdump("C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe");
        cfg.setRutaOrigen("C:\\Backups");
        cfg.setRutaDestino("");
        cfg.setTipoBackup("COMPLETO");
        cfg.setRetencionActiva(false);
        cfg.setDiasRetencion(7);
        cfg.setNumEjecuciones(1);
        cfg.setHoraBackup1(LocalTime.of(8, 0));
        cfg.setHoraBackup2(null);
        cfg.setHoraBackup3(null);
        cfg.setActivo(false);
        cfg.setDestinoLocal(false);
        cfg.setDestinoEmail(false);
        cfg.setTipoDestino("NINGUNO");
        cfg.setNotificarError(true);
        cfg.setNotificarExito(false);
        return configRepo.save(cfg);
    }

    private ConfigBackupDTO toConfigDTO(ConfigBackup cfg) {
        ConfigBackupDTO dto = new ConfigBackupDTO();
        dto.setIdConfig(cfg.getIdConfig());
        dto.setRutaPgdump(cfg.getRutaPgdump());
        dto.setRutaOrigen(cfg.getRutaOrigen());
        dto.setTipoBackup(cfg.getTipoBackup());
        dto.setRetencionActiva(cfg.getRetencionActiva());
        dto.setDiasRetencion(cfg.getDiasRetencion());
        dto.setNumEjecuciones(cfg.getNumEjecuciones());
        dto.setHoraBackup1(cfg.getHoraBackup1() != null
                ? cfg.getHoraBackup1().format(DateTimeFormatter.ofPattern("HH:mm")) : "08:00");
        dto.setHoraBackup2(cfg.getHoraBackup2() != null
                ? cfg.getHoraBackup2().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        dto.setHoraBackup3(cfg.getHoraBackup3() != null
                ? cfg.getHoraBackup3().format(DateTimeFormatter.ofPattern("HH:mm")) : "");
        dto.setActivo(cfg.getActivo());
        dto.setDestinoLocal(Boolean.TRUE.equals(cfg.getDestinoLocal()));
        dto.setDestinoEmail(Boolean.TRUE.equals(cfg.getDestinoEmail()));
        dto.setTipoDestino(cfg.getTipoDestino());
        dto.setRutaDestino(cfg.getRutaDestino());
        dto.setEmailDestino(cfg.getEmailDestino());
        dto.setNotificarError(cfg.getNotificarError());
        dto.setNotificarExito(cfg.getNotificarExito());
        return dto;
    }

    private HistorialBackupDTO toHistorialDTO(HistorialBackup h) {
        HistorialBackupDTO dto = new HistorialBackupDTO();
        dto.setIdHistorial(h.getIdHistorial());
        dto.setEstado(h.getEstado());
        dto.setTipoBackup(h.getTipoBackup());
        dto.setRutaArchivo(h.getRutaArchivo());
        dto.setTamanoBytes(h.getTamanoBytes());
        dto.setDuracionSegundos(h.getDuracionSegundos());
        dto.setMensajeError(h.getMensajeError());
        dto.setOrigen(h.getOrigen());
        dto.setFechaInicio(h.getFechaInicio());
        dto.setFechaFin(h.getFechaFin());
        dto.setTamanoFormateado(h.getTamanoBytes() != null
                ? formatearTamano(h.getTamanoBytes()) : "-");
        dto.setDuracionFormateada(h.getDuracionSegundos() != null
                ? h.getDuracionSegundos() + "s" : "-");
        return dto;
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024)                  return bytes + " B";
        if (bytes < 1024 * 1024)           return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)   return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // Helper para usar Boolean como primitivo en if()
    private boolean isTrue(Boolean b) { return Boolean.TRUE.equals(b); }
}