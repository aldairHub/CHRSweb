package org.uteq.backend.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final GoogleDriveService        driveService;

    @Lazy @Autowired
    private BackupSchedulerService schedulerService;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    // =========================================================================
    // INIT
    // =========================================================================

    @PostConstruct
    public void inicializarScheduler() {
        try {
            ConfigBackup cfg = configRepo.findFirstByOrderByIdConfigAsc().orElse(null);
            if (cfg != null) schedulerService.reconfigurar(toConfigDTO(cfg));
        } catch (Exception e) {
            log.warn("No se pudo inicializar el scheduler: {}", e.getMessage());
        }
    }

    // =========================================================================
    // CONFIG
    // =========================================================================

    public ConfigBackupDTO obtenerConfig() {
        return toConfigDTO(configRepo.findFirstByOrderByIdConfigAsc()
                .orElseGet(this::crearConfigDefecto));
    }

    public ConfigBackupDTO guardarConfig(ConfigBackupDTO dto) {
        ConfigBackup cfg = configRepo.findFirstByOrderByIdConfigAsc()
                .orElse(new ConfigBackup());

        cfg.setRutaPgdump(dto.getRutaPgdump() != null && !dto.getRutaPgdump().isBlank()
                ? dto.getRutaPgdump() : "pg_dump");
        cfg.setRutaOrigen(dto.getRutaOrigen());
        cfg.setTipoBackup(dto.getTipoBackup() != null ? dto.getTipoBackup() : "FULL");
        cfg.setRetencionActiva(dto.getRetencionActiva());
        cfg.setDiasRetencion(dto.getDiasRetencion() != null ? dto.getDiasRetencion() : 7);
        cfg.setActivo(dto.getActivo());
        cfg.setNotificarError(dto.getNotificarError());
        cfg.setNotificarExito(dto.getNotificarExito());

        // Destinos múltiples
        cfg.setDestinoLocal(Boolean.TRUE.equals(dto.getDestinoLocal()));
        cfg.setDestinoEmail(Boolean.TRUE.equals(dto.getDestinoEmail()));
        cfg.setDestinoDrive(Boolean.TRUE.equals(dto.getDestinoDrive()));
        cfg.setRutaDestino(dto.getRutaDestino());
        cfg.setEmailDestino(dto.getEmailDestino());
        cfg.setDriveFolderName(dto.getDriveFolderName());
        cfg.setDriveFolderId(dto.getDriveFolderId());

        // Legacy
        if (Boolean.TRUE.equals(cfg.getDestinoEmail()) && Boolean.TRUE.equals(cfg.getDestinoLocal()))
            cfg.setTipoDestino("LOCAL");
        else if (Boolean.TRUE.equals(cfg.getDestinoEmail())) cfg.setTipoDestino("EMAIL");
        else if (Boolean.TRUE.equals(cfg.getDestinoLocal()))  cfg.setTipoDestino("LOCAL");
        else cfg.setTipoDestino("NINGUNO");

        int num = dto.getNumEjecuciones() != null ? dto.getNumEjecuciones() : 1;
        cfg.setNumEjecuciones(num);
        cfg.setHoraBackup1(dto.getHoraBackup1() != null
                ? LocalTime.parse(dto.getHoraBackup1()) : LocalTime.of(8, 0));
        cfg.setHoraBackup2(num >= 2 && dto.getHoraBackup2() != null && !dto.getHoraBackup2().isBlank()
                ? LocalTime.parse(dto.getHoraBackup2()) : null);
        cfg.setHoraBackup3(num >= 3 && dto.getHoraBackup3() != null && !dto.getHoraBackup3().isBlank()
                ? LocalTime.parse(dto.getHoraBackup3()) : null);

        ConfigBackupDTO resultado = toConfigDTO(configRepo.save(cfg));
        schedulerService.reconfigurar(resultado);
        return resultado;
    }

    // =========================================================================
    // EJECUTAR — puntos de entrada públicos
    // =========================================================================

    /** Backup FULL manual (backward compatible) */
    public HistorialBackupDTO ejecutarBackupManual() {
        return ejecutarBackup(obtenerOCrearConfig(), "MANUAL", "FULL");
    }

    /** Backup de tipo específico (FULL | INCREMENTAL | DIFERENCIAL) */
    public HistorialBackupDTO ejecutarBackupTipo(String tipo) {
        String t = tipo != null ? tipo.toUpperCase() : "FULL";
        if (!List.of("FULL", "INCREMENTAL", "DIFERENCIAL").contains(t)) t = "FULL";
        return ejecutarBackup(obtenerOCrearConfig(), "MANUAL", t);
    }

    /** Llamado por el scheduler automático */
    public HistorialBackupDTO ejecutarBackupAutomatico() {
        ConfigBackup cfg = obtenerOCrearConfig();
        String tipo = determinarTipoAutomatico(cfg.getTipoBackup());
        return ejecutarBackup(cfg, "AUTOMATICO", tipo);
    }

    // =========================================================================
    // LÓGICA PRINCIPAL
    // =========================================================================

    private HistorialBackupDTO ejecutarBackup(ConfigBackup cfg, String origen, String tipoExt) {
        HistorialBackup historial = new HistorialBackup();
        historial.setFechaInicio(LocalDateTime.now());
        historial.setTipoBackup(cfg.getTipoBackup());
        historial.setTipoBackupExt(tipoExt);
        historial.setOrigen(origen);
        historial.setEmailEnviado(false);
        historial.setDriveSubido(false);

        long inicio = System.currentTimeMillis();

        try {
            crearCarpetaSiNoExiste(cfg.getRutaOrigen());

            String timestamp  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
            String sufijo     = "_" + tipoExt.toLowerCase();
            String nombreDump = "backup" + sufijo + "_" + timestamp + ".dump";
            String rutaDump   = cfg.getRutaOrigen() + File.separator + nombreDump;
            String nombreZip  = "backup" + sufijo + "_" + timestamp + ".zip";
            String rutaZip    = cfg.getRutaOrigen() + File.separator + nombreZip;

            // pg_dump
            ejecutarPgDump(cfg.getRutaPgdump(), rutaDump);

            // Comprimir y limpiar .dump
            comprimirArchivo(rutaDump, rutaZip);
            Files.deleteIfExists(Paths.get(rutaDump));

            long tamano   = Files.size(Paths.get(rutaZip));
            long duracion = (System.currentTimeMillis() - inicio) / 1000;

            // ── Destino LOCAL secundario ───────────────────────────
            if (Boolean.TRUE.equals(cfg.getDestinoLocal())
                    && cfg.getRutaDestino() != null && !cfg.getRutaDestino().isBlank()) {
                try {
                    crearCarpetaSiNoExiste(cfg.getRutaDestino());
                    Files.copy(Paths.get(rutaZip),
                            Paths.get(cfg.getRutaDestino(), nombreZip),
                            StandardCopyOption.REPLACE_EXISTING);
                    log.info("Backup copiado a ruta local: {}", cfg.getRutaDestino());
                } catch (Exception e) {
                    log.warn("Error al copiar a ruta local: {}", e.getMessage());
                }
            }

            // ── Destino EMAIL ──────────────────────────────────────
            if (Boolean.TRUE.equals(cfg.getDestinoEmail())) {
                try {
                    enviarBackupPorEmail(cfg.getEmailDestino(), rutaZip, nombreZip, tamano);
                    historial.setEmailEnviado(true);
                } catch (Exception e) {
                    log.warn("Error al enviar email: {}", e.getMessage());
                    historial.setEmailEnviado(false);
                }
            }

            // ── Destino GOOGLE DRIVE ───────────────────────────────
            if (Boolean.TRUE.equals(cfg.getDestinoDrive()) && driveService.estaListo()) {
                try {
                    String folderId = cfg.getDriveFolderId();
                    String folderName = (cfg.getDriveFolderName() != null && !cfg.getDriveFolderName().isBlank())
                            ? cfg.getDriveFolderName() : "SSDC-Backups";
                    if (folderId == null || folderId.isBlank()) {
                        folderId = driveService.obtenerOCrearCarpeta(folderName);
                        cfg.setDriveFolderId(folderId);
                        configRepo.save(cfg);
                    }
                    GoogleDriveService.DriveUploadResult result =
                            driveService.subirArchivo(new File(rutaZip), folderId, "application/zip");
                    historial.setDriveFileId(result.fileId());
                    historial.setDriveUrl(result.webViewLink());
                    historial.setDriveSubido(true);
                    log.info("Backup subido a Drive: {} ({})", nombreZip, result.fileId());
                } catch (Exception e) {
                    log.warn("Error al subir a Drive: {}", e.getMessage());
                    historial.setDriveSubido(false);
                }
            }

            historial.setEstado("EXITOSO");
            historial.setRutaArchivo(rutaZip);
            historial.setTamanoBytes(tamano);
            historial.setDuracionSegundos(duracion);
            historial.setFechaFin(LocalDateTime.now());
            historialRepo.save(historial);

            if (Boolean.TRUE.equals(cfg.getRetencionActiva())) limpiarBackupsAntiguos(cfg);

            if (Boolean.TRUE.equals(cfg.getNotificarExito())) {
                try {
                    notificacionService.notificarRol("admin", "success",
                            "Backup " + tipoExt + " exitoso ✅",
                            "Completado: " + nombreZip + " (" + formatearTamano(tamano) + ") en " + duracion + "s.",
                            "BACKUP", historial.getIdHistorial());
                } catch (Exception e) { log.warn("No se pudo notificar éxito: {}", e.getMessage()); }
            }
            log.info("Backup [{}] exitoso: {} en {}s", tipoExt, nombreZip, duracion);

        } catch (Exception e) {
            long duracion = (System.currentTimeMillis() - inicio) / 1000;
            historial.setEstado("FALLIDO");
            historial.setMensajeError(e.getMessage());
            historial.setDuracionSegundos(duracion);
            historial.setFechaFin(LocalDateTime.now());
            historialRepo.save(historial);

            if (Boolean.TRUE.equals(cfg.getNotificarError())) {
                try {
                    notificacionService.notificarRol("admin", "error",
                            "Error en backup " + tipoExt + " ❌",
                            "El backup falló: " + e.getMessage(),
                            "BACKUP", null);
                } catch (Exception ne) { log.warn("No se pudo notificar error: {}", ne.getMessage()); }
            }
            log.error("Backup [{}] falló: {}", tipoExt, e.getMessage());
        }

        return toHistorialDTO(historial);
    }

    // =========================================================================
    // DETERMINAR TIPO AUTOMÁTICO
    // =========================================================================

    /**
     * FULL     → siempre FULL
     * INCREMENTAL → FULL si no hay ningún backup exitoso previo, sino INCREMENTAL
     * DIFERENCIAL → FULL si no hay ningún FULL exitoso previo, sino DIFERENCIAL
     */
    private String determinarTipoAutomatico(String tipoConfig) {
        if (tipoConfig == null || "FULL".equalsIgnoreCase(tipoConfig)) return "FULL";
        if ("INCREMENTAL".equalsIgnoreCase(tipoConfig)) {
            boolean hayAnterior = historialRepo
                    .findTop1ByEstadoOrderByFechaInicioDesc("EXITOSO").isPresent();
            return hayAnterior ? "INCREMENTAL" : "FULL";
        }
        if ("DIFERENCIAL".equalsIgnoreCase(tipoConfig)) {
            boolean hayFull = historialRepo
                    .findTop1ByTipoBackupExtAndEstadoOrderByFechaInicioDesc("FULL", "EXITOSO")
                    .isPresent();
            return hayFull ? "DIFERENCIAL" : "FULL";
        }
        return "FULL";
    }

    // =========================================================================
    // pg_dump
    // =========================================================================

    private void ejecutarPgDump(String rutaPgdump, String rutaSalida)
            throws IOException, InterruptedException {

        String clean  = datasourceUrl.replace("jdbc:postgresql://", "").split("\\?")[0];
        String[] partes = clean.split("/");
        String dbname   = partes[partes.length - 1];
        String[] hp     = partes[0].split(":");
        String host     = hp[0];
        String puerto   = hp.length > 1 ? hp[1] : "5432";
        String pgDumpExe = (rutaPgdump != null && !rutaPgdump.isBlank()) ? rutaPgdump : "pg_dump";

        ProcessBuilder pb = new ProcessBuilder(
                pgDumpExe,
                "-h", host, "-p", puerto, "-U", datasourceUsername,
                "--no-owner", "--no-acl", "--schema=public",
                "-F", "c", "-f", rutaSalida, dbname);
        pb.environment().put("PGPASSWORD", datasourcePassword);
        pb.redirectErrorStream(true);

        Process proceso  = pb.start();
        String  output   = new String(proceso.getInputStream().readAllBytes());
        int     exitCode = proceso.waitFor();
        if (exitCode != 0)
            throw new RuntimeException("pg_dump falló (código " + exitCode + "): " + output);
    }

    // =========================================================================
    // EMAIL
    // =========================================================================

    private void enviarBackupPorEmail(String destino, String rutaZip,
                                      String nombreZip, long tamano) throws Exception {
        if (destino == null || destino.isBlank())
            throw new RuntimeException("No se configuró email de destino.");

        List<String> emails = Arrays.stream(destino.split("[,;]"))
                .map(String::trim).filter(e -> !e.isEmpty()).collect(Collectors.toList());
        if (emails.isEmpty()) throw new RuntimeException("No hay emails válidos configurados.");

        MimeMessage message = dynamicMailService.getMailSender().createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(dynamicMailService.getEmailFrom());
        helper.setTo(emails.toArray(new String[0]));
        helper.setSubject("🗄️ Backup BD — " + nombreZip);
        helper.setText(
                "<div style='font-family:-apple-system,sans-serif'>" +
                "<h3 style='color:#00A63E'>Backup de Base de Datos</h3>" +
                "<p>Se adjunta el backup generado automáticamente.</p>" +
                "<table style='border-collapse:collapse;font-size:14px'>" +
                "<tr><td style='padding:4px 16px 4px 0;color:#6b7280'>Archivo:</td><td><b>" + nombreZip + "</b></td></tr>" +
                "<tr><td style='padding:4px 16px 4px 0;color:#6b7280'>Tamaño:</td><td>" + formatearTamano(tamano) + "</td></tr>" +
                "<tr><td style='padding:4px 16px 4px 0;color:#6b7280'>Fecha:</td><td>" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                "</td></tr></table></div>", true);
        helper.addAttachment(nombreZip, new FileSystemResource(new File(rutaZip)));
        dynamicMailService.getMailSender().send(message);
        log.info("Backup enviado por email a: {}", String.join(", ", emails));
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void crearCarpetaSiNoExiste(String ruta) throws IOException {
        if (ruta == null || ruta.isBlank()) return;
        Path path = Paths.get(ruta);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("Carpeta creada: {}", ruta);
        }
    }

    private void comprimirArchivo(String rutaArchivo, String rutaZip) throws IOException {
        Path archivo = Paths.get(rutaArchivo);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(Paths.get(rutaZip)))) {
            zos.putNextEntry(new ZipEntry(archivo.getFileName().toString()));
            Files.copy(archivo, zos);
            zos.closeEntry();
        }
    }

    private void limpiarBackupsAntiguos(ConfigBackup cfg) {
        try {
            File carpeta = new File(cfg.getRutaOrigen());
            File[] zips  = carpeta.listFiles((d, n) -> n.endsWith(".zip"));
            if (zips == null) return;
            LocalDateTime limite = LocalDateTime.now().minusDays(cfg.getDiasRetencion());
            for (File f : zips) {
                LocalDateTime fecha = LocalDateTime.ofInstant(
                        java.nio.file.attribute.FileTime
                                .fromMillis(f.lastModified()).toInstant(), ZoneId.systemDefault());
                if (fecha.isBefore(limite)) {
                    f.delete();
                    log.info("Backup antiguo eliminado: {}", f.getName());
                }
            }
        } catch (Exception e) { log.warn("Error limpiando backups: {}", e.getMessage()); }
    }

    public List<HistorialBackupDTO> obtenerHistorial() {
        return historialRepo.findTop50ByOrderByFechaInicioDesc()
                .stream().map(this::toHistorialDTO).collect(Collectors.toList());
    }

    // =========================================================================
    // MAPPERS
    // =========================================================================

    private ConfigBackup obtenerOCrearConfig() {
        return configRepo.findFirstByOrderByIdConfigAsc().orElseGet(this::crearConfigDefecto);
    }

    private ConfigBackup crearConfigDefecto() {
        ConfigBackup cfg = new ConfigBackup();
        cfg.setRutaPgdump("pg_dump");
        cfg.setRutaOrigen("C:\\Backups");
        cfg.setTipoBackup("FULL");
        cfg.setRetencionActiva(false);
        cfg.setDiasRetencion(7);
        cfg.setNumEjecuciones(1);
        cfg.setHoraBackup1(LocalTime.of(8, 0));
        cfg.setActivo(false);
        cfg.setDestinoLocal(false);
        cfg.setDestinoEmail(false);
        cfg.setDestinoDrive(false);
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
        dto.setDestinoDrive(Boolean.TRUE.equals(cfg.getDestinoDrive()));
        dto.setTipoDestino(cfg.getTipoDestino());
        dto.setRutaDestino(cfg.getRutaDestino());
        dto.setEmailDestino(cfg.getEmailDestino());
        dto.setDriveFolderName(cfg.getDriveFolderName());
        dto.setDriveFolderId(cfg.getDriveFolderId());
        dto.setNotificarError(cfg.getNotificarError());
        dto.setNotificarExito(cfg.getNotificarExito());
        return dto;
    }

    private HistorialBackupDTO toHistorialDTO(HistorialBackup h) {
        HistorialBackupDTO dto = new HistorialBackupDTO();
        dto.setIdHistorial(h.getIdHistorial());
        dto.setEstado(h.getEstado());
        dto.setTipoBackup(h.getTipoBackup());
        dto.setTipoBackupExt(h.getTipoBackupExt() != null ? h.getTipoBackupExt() : "FULL");
        dto.setRutaArchivo(h.getRutaArchivo());
        dto.setTamanoBytes(h.getTamanoBytes());
        dto.setDuracionSegundos(h.getDuracionSegundos());
        dto.setMensajeError(h.getMensajeError());
        dto.setOrigen(h.getOrigen());
        dto.setFechaInicio(h.getFechaInicio());
        dto.setFechaFin(h.getFechaFin());
        dto.setTamanoFormateado(h.getTamanoBytes() != null ? formatearTamano(h.getTamanoBytes()) : "-");
        dto.setDuracionFormateada(h.getDuracionSegundos() != null ? h.getDuracionSegundos() + "s" : "-");
        dto.setDriveFileId(h.getDriveFileId());
        dto.setDriveUrl(h.getDriveUrl());
        dto.setDriveSubido(Boolean.TRUE.equals(h.getDriveSubido()));
        dto.setEmailEnviado(Boolean.TRUE.equals(h.getEmailEnviado()));
        dto.setLsnFin(h.getLsnFin());
        return dto;
    }

    private String formatearTamano(long bytes) {
        if (bytes < 1024)                  return bytes + " B";
        if (bytes < 1024 * 1024)           return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024)   return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
