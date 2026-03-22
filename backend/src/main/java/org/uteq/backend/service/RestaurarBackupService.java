package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.repository.ConfigBackupRepository;

import com.zaxxer.hikari.HikariDataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Qualifier;

@Service
public class RestaurarBackupService {

    private static final Logger log = LoggerFactory.getLogger(RestaurarBackupService.class);

    private final ConfigBackupRepository configRepo;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${backup.postgres.username:postgres}")
    private String postgresUsername;

    @Value("${backup.postgres.password}")
    private String postgresPassword;

    private final org.uteq.backend.service.AesCipherService aesCipherService;
    private final HikariDataSource baseDataSource;

    public RestaurarBackupService(
            ConfigBackupRepository configRepo,
            org.uteq.backend.service.AesCipherService aesCipherService,
            @Qualifier("baseHikariDataSource") HikariDataSource baseDataSource) {
        this.configRepo    = configRepo;
        this.aesCipherService = aesCipherService;
        this.baseDataSource   = baseDataSource;
    }

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

            // 4. DROP + CREATE de la BD para restaurar en limpio
            recrearBaseDeDatos(host, puerto, dbname);

            // 5. Ejecutar pg_restore
            ProcessBuilder pb = new ProcessBuilder(
                    pgRestoreExe,
                    "--clean",          // DROP antes de CREATE
                    "--if-exists",      // no falla si un objeto no existe
                    "--no-owner",
                    "--no-acl",
                    "--schema=public",
                    "-h", host,
                    "-p", puerto,
                    "-U", postgresUsername,  // usar postgres, no readonly_user
                    "-d", dbname,
                    tmpDump.toAbsolutePath().toString()
            );
            pb.environment().put("PGPASSWORD", postgresPassword);
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

            // Ejecutar SP de post-restauración para recrear todos los permisos
            ejecutarPostRestauracion(host, puerto, dbname);

            // Forzar reconexión del pool para que los nuevos permisos apliquen
            reiniciarPool();

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

    // =========================================================================
    // DROP + CREATE de la BD — conecta a 'postgres' para poder borrar ssdc_loc
    // =========================================================================

    private void recrearBaseDeDatos(String host, String puerto, String dbname) throws Exception {
        String jdbcPostgres = "jdbc:postgresql://" + host + ":" + puerto + "/postgres?prepareThreshold=0";
        log.info("Recreando BD '{}' para restore limpio...", dbname);

        try (Connection conn = DriverManager.getConnection(jdbcPostgres, postgresUsername, postgresPassword);
             Statement st = conn.createStatement()) {

            // Terminar todas las conexiones activas a la BD (excepto la actual)
            st.execute(
                    "SELECT pg_terminate_backend(pid) " +
                            "FROM pg_stat_activity " +
                            "WHERE datname = '" + dbname + "' AND pid <> pg_backend_pid()"
            );

            // DROP y CREATE
            st.execute("DROP DATABASE IF EXISTS \"" + dbname + "\"");
            st.execute("CREATE DATABASE \"" + dbname + "\"");

            log.info("BD '{}' recreada correctamente para restore limpio.", dbname);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo recrear la BD '" + dbname + "': " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // REINICIO DEL POOL — fuerza nuevas conexiones con permisos actualizados
    // =========================================================================

    private void reiniciarPool() {
        try {
            log.info("Reiniciando pool de conexiones para aplicar nuevos permisos...");
            baseDataSource.getHikariPoolMXBean().softEvictConnections();
            log.info("Pool reiniciado exitosamente.");
        } catch (Exception e) {
            log.warn("No se pudo reiniciar el pool: {}", e.getMessage());
        }
    }

    // =========================================================================
    // POST-RESTAURACIÓN — llama al SP que recrea todos los permisos por rol
    // =========================================================================

    private void ejecutarPostRestauracion(String host, String puerto, String dbname) {
        // Usar los mismos parámetros SSL que el datasource original
        String sslParams = datasourceUrl.contains("sslmode=require")
                ? "?sslmode=require&prepareThreshold=0"
                : "?prepareThreshold=0";
        String jdbcUrl = "jdbc:postgresql://" + host + ":" + puerto + "/" + dbname + sslParams;
        try (Connection conn = DriverManager.getConnection(jdbcUrl, postgresUsername, postgresPassword);
             Statement st = conn.createStatement()) {

            // 1. Ejecutar SP de permisos por rol
            st.execute("CALL public.sp_post_restauracion()");
            log.info("SP post-restauracion ejecutado: permisos recreados exitosamente.");

            // 2. Recrear usuarios individuales de BD que no existen en PostgreSQL
            recrearUsuariosBd(conn);

        } catch (Exception e) {
            log.warn("No se pudo ejecutar post-restauracion: {}. Ejecuta manualmente: CALL public.sp_post_restauracion()", e.getMessage());
        }
    }

    private void recrearUsuariosBd(Connection conn) {
        // Trae usuario_bd, clave_bd y todos los roles BD que necesita
        String sql = "SELECT DISTINCT u.usuario_bd, u.clave_bd, rab.nombre_rol_bd "
                + "FROM usuario u "
                + "JOIN usuario_roles_app ura ON ura.id_usuario = u.id_usuario "
                + "JOIN roles_app_bd rab ON rab.id_rol_app = ura.id_rol_app "
                + "WHERE u.usuario_bd IS NOT NULL "
                + "AND u.usuario_bd NOT IN ('admin', 'readonly_user', 'postgres', 'pgbouncer')";

        // Agrupar por usuario: usuario_bd -> {clave_bd, [roles]}
        java.util.Map<String, String[]> usuarios = new java.util.LinkedHashMap<>();
        java.util.Map<String, java.util.List<String>> rolesMap = new java.util.LinkedHashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String usuarioBd    = rs.getString("usuario_bd");
                String claveCifrada = rs.getString("clave_bd");
                String rolBd        = rs.getString("nombre_rol_bd");
                if (usuarioBd == null) continue;
                usuarios.put(usuarioBd, new String[]{claveCifrada});
                rolesMap.computeIfAbsent(usuarioBd, k -> new java.util.ArrayList<>()).add(rolBd);
            }
        } catch (Exception e) {
            log.warn("Error al leer usuarios BD: {}", e.getMessage());
            return;
        }

        int creados = 0;
        for (java.util.Map.Entry<String, String[]> entry : usuarios.entrySet()) {
            String usuarioBd    = entry.getKey();
            String claveCifrada = entry.getValue()[0];
            if (claveCifrada == null) continue;

            try {
                String claveReal = aesCipherService.descifrar(claveCifrada);
                try (Statement st2 = conn.createStatement()) {
                    // Crear usuario si no existe
                    boolean existe = false;
                    try (PreparedStatement chk = conn.prepareStatement(
                            "SELECT 1 FROM pg_roles WHERE rolname = ?")) {
                        chk.setString(1, usuarioBd);
                        ResultSet r2 = chk.executeQuery();
                        existe = r2.next();
                    }

                    if (!existe) {
                        st2.execute("CREATE USER \"" + usuarioBd.replace("\"", "") + "\" WITH PASSWORD '"
                                + claveReal.replace("'", "''") + "'");
                        creados++;
                    } else {
                        // Solo actualizar contraseña
                        st2.execute("ALTER USER \"" + usuarioBd.replace("\"", "") + "\" WITH PASSWORD '"
                                + claveReal.replace("'", "''") + "'");
                    }

                    // Asignar base_app_permisos
                    try { st2.execute("GRANT base_app_permisos TO \"" + usuarioBd.replace("\"", "") + "\""); }
                    catch (Exception ignored) {}

                    // Asignar todos sus roles BD
                    java.util.List<String> roles = rolesMap.getOrDefault(usuarioBd, java.util.Collections.emptyList());
                    for (String rol : roles) {
                        if (rol != null && !rol.isBlank()) {
                            try { st2.execute("GRANT \"" + rol.replace("\"", "") + "\" TO \"" + usuarioBd.replace("\"", "") + "\""); }
                            catch (Exception ignored) {}
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("No se pudo procesar usuario BD '{}': {}", usuarioBd, ex.getMessage());
            }
        }
        log.info("Usuarios BD procesados tras restauracion: {} creados de {} total.", creados, usuarios.size());
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