package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.repository.ConfigBackupRepository;

import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class RestaurarBackupService {

    private static final Logger log = LoggerFactory.getLogger(RestaurarBackupService.class);

    private final ConfigBackupRepository configRepo;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    // Tablas que DEBEN existir en el backup para considerarlo válido
    private static final List<String> TABLAS_REQUERIDAS = Arrays.asList(
        "usuario", "institucion", "convocatoria", "postulante",
        "roles_app", "notificacion", "config_backup", "historial_backup",
        "facultad", "carrera", "solicitud_docente"
    );

    // =========================================================================
    // VALIDAR
    // =========================================================================

    public void validarBackup(MultipartFile archivo) throws Exception {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("No se recibió ningún archivo.");
        }

        String nombre = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().toLowerCase() : "";

        if (!nombre.endsWith(".zip") && !nombre.endsWith(".dump")) {
            throw new IllegalArgumentException(
                "Formato inválido. Solo se aceptan archivos .zip o .dump generados por esta aplicación.");
        }

        // Extraer el .dump del .zip si es necesario
        byte[] dumpBytes = nombre.endsWith(".zip")
                ? extraerDumpDeZip(archivo.getBytes())
                : archivo.getBytes();

        if (dumpBytes == null || dumpBytes.length == 0) {
            throw new IllegalArgumentException(
                "El archivo ZIP no contiene un dump válido.");
        }

        // pg_restore --list permite leer la tabla de contenidos sin restaurar
        validarEstructura(dumpBytes);
    }

    // =========================================================================
    // RESTAURAR
    // =========================================================================

    public void restaurar(MultipartFile archivo) throws Exception {
        // 1. Validar primero
        validarBackup(archivo);

        String nombre = archivo.getOriginalFilename() != null
                ? archivo.getOriginalFilename().toLowerCase() : "";

        byte[] dumpBytes = nombre.endsWith(".zip")
                ? extraerDumpDeZip(archivo.getBytes())
                : archivo.getBytes();

        // 2. Escribir dump a archivo temporal
        Path tmpDump = Files.createTempFile("restore_", ".dump");
        try {
            Files.write(tmpDump, dumpBytes);

            // 3. Parsear conexión
            String clean  = datasourceUrl.replace("jdbc:postgresql://", "").split("\\?")[0];
            String[] pts  = clean.split("/");
            String dbname = pts[pts.length - 1];
            String[] hp   = pts[0].split(":");
            String host   = hp[0];
            String puerto = hp.length > 1 ? hp[1] : "5432";

            String pgRestoreExe = obtenerPgRestore();

            // 4. Ejecutar pg_restore
            ProcessBuilder pb = new ProcessBuilder(
                pgRestoreExe,
                "--clean",          // DROP antes de CREATE
                "--if-exists",      // no falla si un objeto no existe
                "--no-owner",
                "--no-acl",
                "--schema=public",
                "-h", host,
                "-p", puerto,
                "-U", datasourceUsername,
                "-d", dbname,
                tmpDump.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);

            Process proceso  = pb.start();
            String  output   = new String(proceso.getInputStream().readAllBytes());
            int     exitCode = proceso.waitFor();

            // pg_restore devuelve 1 si hay advertencias no fatales — aceptamos eso
            if (exitCode > 1) {
                throw new RuntimeException(
                    "pg_restore falló (código " + exitCode + "): " + output);
            }
            log.info("Restauración completada. Salida: {}", output.length() > 500
                    ? output.substring(0, 500) + "..." : output);

        } finally {
            Files.deleteIfExists(tmpDump);
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private byte[] extraerDumpDeZip(byte[] zipBytes) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".dump")) {
                    return zis.readAllBytes();
                }
            }
        }
        return null;
    }

    private void validarEstructura(byte[] dumpBytes) throws Exception {
        Path tmpDump = Files.createTempFile("validate_", ".dump");
        try {
            Files.write(tmpDump, dumpBytes);

            String pgRestoreExe = obtenerPgRestore();

            // pg_restore --list imprime el TOC sin conectar a la BD
            ProcessBuilder pb = new ProcessBuilder(
                pgRestoreExe,
                "--list",
                tmpDump.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);

            Process proceso  = pb.start();
            String  toc      = new String(proceso.getInputStream().readAllBytes());
            int     exitCode = proceso.waitFor();

            if (exitCode != 0) {
                throw new IllegalArgumentException(
                    "El archivo no es un dump de PostgreSQL válido (formato pg_custom).");
            }

            // Verificar que contiene tablas clave del sistema
            String tocLower = toc.toLowerCase();
            List<String> faltantes = TABLAS_REQUERIDAS.stream()
                .filter(t -> !tocLower.contains(t))
                .collect(java.util.stream.Collectors.toList());

            if (!faltantes.isEmpty()) {
                throw new IllegalArgumentException(
                    "El backup no pertenece a esta aplicación. " +
                    "Faltan las tablas: " + String.join(", ", faltantes) + ". " +
                    "Solo se pueden restaurar backups generados por este sistema.");
            }

            log.info("Validación OK: todas las tablas requeridas encontradas en el TOC.");

        } finally {
            Files.deleteIfExists(tmpDump);
        }
    }

    private String obtenerPgRestore() {
        // Derivar pg_restore desde la ruta de pg_dump configurada
        String pgDump = configRepo.findFirstByOrderByIdConfigAsc()
                .map(c -> c.getRutaPgdump())
                .orElse("pg_dump");

        if (pgDump == null || pgDump.isBlank() || pgDump.equals("pg_dump")) {
            return "pg_restore";
        }

        // Reemplazar pg_dump.exe → pg_restore.exe
        return pgDump
                .replace("pg_dump.exe", "pg_restore.exe")
                .replace("pg_dump",     "pg_restore");
    }
}
