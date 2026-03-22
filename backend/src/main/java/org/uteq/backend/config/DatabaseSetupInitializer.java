package org.uteq.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class DatabaseSetupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSetupInitializer.class);
    private final JdbcTemplate jdbc;

    public DatabaseSetupInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(">>> [DatabaseSetupInitializer] Verificando BD...");
        ejecutarSetup();
        log.info(">>> [DatabaseSetupInitializer] Listo.");
    }

    // Idempotente: todo usa IF NOT EXISTS / ON CONFLICT / ejecutarSafe
    private void ejecutarSetup() {
        crearRolesPostgres();
        ajustarColumnas();
        crearTablasFaltantes();
        asignarPermisos();
        crearRolesApp();
        crearStoredProcedures();
        ejecutarScriptSql();
        limpiarHuerfanos();
    }

    // =========================================================
    // ROLES POSTGRESQL
    // =========================================================
    private void crearRolesPostgres() {
        String[] roles = {
                "DO $$ BEGIN CREATE USER readonly_user WITH PASSWORD 'readonly_ssdc'; EXCEPTION WHEN duplicate_object THEN NULL; END $$",
                "DO $$ BEGIN CREATE ROLE base_app_permisos  NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END $$",
                "DO $$ BEGIN CREATE ROLE role_admin_bd      NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END $$",
                "DO $$ BEGIN CREATE ROLE role_lecturas      NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END $$",
                "DO $$ BEGIN CREATE ROLE role_escritura     NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END $$",
                "DO $$ BEGIN CREATE ROLE role_postulante_bd NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END $$",
                "DO $$ BEGIN CREATE ROLE role_evaluador_bd  NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END $$",
                "DO $$ BEGIN CREATE ROLE role_revisor_bd    NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END $$"
        };
        for (String sql : roles) ejecutarSafe(sql);
    }

    // =========================================================
    // COLUMNAS OPCIONALES (idempotente con ADD COLUMN IF NOT EXISTS)
    // =========================================================
    private void ajustarColumnas() {
        ejecutarSafe("ALTER TABLE config_backup ALTER COLUMN hora_backup_2 DROP NOT NULL");
        ejecutarSafe("ALTER TABLE config_backup ALTER COLUMN hora_backup_3 DROP NOT NULL");
        ejecutarSafe("ALTER TABLE config_backup ALTER COLUMN ruta_destino  DROP NOT NULL");
        ejecutarSafe("ALTER TABLE config_backup ALTER COLUMN email_destino DROP NOT NULL");
        ejecutarSafe("ALTER TABLE config_backup ADD COLUMN IF NOT EXISTS destino_local     BOOLEAN NOT NULL DEFAULT false");
        ejecutarSafe("ALTER TABLE config_backup ADD COLUMN IF NOT EXISTS destino_email     BOOLEAN NOT NULL DEFAULT false");
        ejecutarSafe("ALTER TABLE config_backup ADD COLUMN IF NOT EXISTS destino_drive     BOOLEAN NOT NULL DEFAULT false");
        ejecutarSafe("ALTER TABLE config_backup ADD COLUMN IF NOT EXISTS drive_folder_name VARCHAR(255)");
        ejecutarSafe("ALTER TABLE config_backup ADD COLUMN IF NOT EXISTS drive_folder_id   TEXT");
        ejecutarSafe("ALTER TABLE historial_backup ADD COLUMN IF NOT EXISTS tipo_backup_ext VARCHAR(20) DEFAULT 'FULL'");
        ejecutarSafe("ALTER TABLE historial_backup ADD COLUMN IF NOT EXISTS drive_file_id   TEXT");
        ejecutarSafe("ALTER TABLE historial_backup ADD COLUMN IF NOT EXISTS drive_url       TEXT");
        ejecutarSafe("ALTER TABLE historial_backup ADD COLUMN IF NOT EXISTS email_enviado   BOOLEAN NOT NULL DEFAULT false");
        ejecutarSafe("ALTER TABLE historial_backup ADD COLUMN IF NOT EXISTS drive_subido    BOOLEAN NOT NULL DEFAULT false");
        ejecutarSafe("ALTER TABLE historial_backup ADD COLUMN IF NOT EXISTS lsn_fin         TEXT");
        ejecutarSafe("ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS token_version   INTEGER NOT NULL DEFAULT 1");
        ejecutarSafe("ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS foto_perfil_url TEXT");
        ejecutarSafe("ALTER TABLE public.prepostulacion ALTER COLUMN url_prerrequisitos DROP NOT NULL");
        ejecutarSafe("ALTER TABLE public.convocatoria ADD COLUMN IF NOT EXISTS fecha_limite_documentos DATE");
        ejecutarSafe("ALTER TABLE public.convocatoria ADD COLUMN IF NOT EXISTS imagen_portada_url TEXT");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS logo_url         TEXT");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS email_smtp       VARCHAR(255)");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS email_password   TEXT");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS email_host       VARCHAR(100) DEFAULT 'smtp.gmail.com'");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS email_port       INT DEFAULT 587");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS email_ssl        BOOLEAN NOT NULL DEFAULT false");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS app_name         VARCHAR(100)");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS activo           BOOLEAN DEFAULT true");
        ejecutarSafe("ALTER TABLE public.institucion ADD COLUMN IF NOT EXISTS imagen_fondo_url TEXT");
        ejecutarSafe("ALTER TABLE public.autoridad_academica ADD COLUMN IF NOT EXISTS id_facultad BIGINT REFERENCES facultad(id_facultad) ON DELETE SET NULL");
    }

    // =========================================================
    // TABLAS EXTRA
    // =========================================================
    private void crearTablasFaltantes() {
        ejecutarSafe(
                "CREATE TABLE IF NOT EXISTS public.config_backup (" +
                        "  id_config      BIGSERIAL    PRIMARY KEY," +
                        "  hora_backup_1  TIME         NOT NULL," +
                        "  hora_backup_2  TIME," +
                        "  hora_backup_3  TIME," +
                        "  ruta_destino   TEXT," +
                        "  email_destino  TEXT," +
                        "  activo         BOOLEAN      NOT NULL DEFAULT true," +
                        "  destino_local  BOOLEAN      NOT NULL DEFAULT false," +
                        "  destino_email  BOOLEAN      NOT NULL DEFAULT false," +
                        "  destino_drive  BOOLEAN      NOT NULL DEFAULT false," +
                        "  drive_folder_name VARCHAR(255)," +
                        "  drive_folder_id   TEXT" +
                        ")"
        );
        ejecutarSafe(
                "CREATE TABLE IF NOT EXISTS public.historial_backup (" +
                        "  id_historial      BIGSERIAL    PRIMARY KEY," +
                        "  fecha_inicio      TIMESTAMPTZ  NOT NULL DEFAULT NOW()," +
                        "  fecha_fin         TIMESTAMPTZ," +
                        "  estado            VARCHAR(20)  NOT NULL DEFAULT 'EN_PROCESO'," +
                        "  tipo_backup       VARCHAR(20)  NOT NULL DEFAULT 'FULL'," +
                        "  tipo_backup_ext   VARCHAR(20)  DEFAULT 'FULL'," +
                        "  origen            VARCHAR(20)  DEFAULT 'MANUAL'," +
                        "  ruta_archivo      TEXT," +
                        "  tamano_bytes      BIGINT," +
                        "  duracion_segundos BIGINT," +
                        "  mensaje_error     TEXT," +
                        "  drive_file_id     TEXT," +
                        "  drive_url         TEXT," +
                        "  email_enviado     BOOLEAN      NOT NULL DEFAULT false," +
                        "  drive_subido      BOOLEAN      NOT NULL DEFAULT false," +
                        "  lsn_fin           TEXT" +
                        ")"
        );
        ejecutarSafe(
                "CREATE TABLE IF NOT EXISTS public.sesiones_activas_app (" +
                        "  id_sesion     BIGSERIAL    PRIMARY KEY," +
                        "  usuario_app   VARCHAR(255) NOT NULL," +
                        "  token_version INT          NOT NULL," +
                        "  ip_cliente    VARCHAR(45)," +
                        "  user_agent    TEXT," +
                        "  fecha_login   TIMESTAMPTZ  DEFAULT NOW()," +
                        "  fecha_cierre  TIMESTAMPTZ," +
                        "  activo        BOOLEAN      DEFAULT true," +
                        "  motivo_cierre VARCHAR(50)" +
                        ")"
        );
        ejecutarSafe("CREATE INDEX IF NOT EXISTS idx_sesiones_activo ON public.sesiones_activas_app (activo, usuario_app)");
        ejecutarSafe(
                "CREATE OR REPLACE VIEW public.v_sesiones_activas AS " +
                        "SELECT s.id_sesion, s.usuario_app, s.ip_cliente, s.fecha_login," +
                        "  EXTRACT(EPOCH FROM (NOW()-s.fecha_login))/60 AS minutos_activo," +
                        "  s.token_version, s.user_agent " +
                        "FROM public.sesiones_activas_app s WHERE s.activo=true ORDER BY s.fecha_login DESC"
        );
        ejecutarSafe(
                "CREATE TABLE IF NOT EXISTS public.prepostulacion_documentos (" +
                        "  id_documento      BIGSERIAL    PRIMARY KEY," +
                        "  id_prepostulacion BIGINT       NOT NULL REFERENCES public.prepostulacion(id_prepostulacion) ON DELETE CASCADE," +
                        "  descripcion       VARCHAR(300) NOT NULL," +
                        "  url_documento     TEXT         NOT NULL," +
                        "  fecha_subida      TIMESTAMPTZ  NOT NULL DEFAULT NOW()" +
                        ")"
        );
        ejecutarSafe(
                "CREATE TABLE IF NOT EXISTS public.aud_cambio (" +
                        "  id_aud_cambio   BIGSERIAL    PRIMARY KEY," +
                        "  tabla           VARCHAR(100) NOT NULL," +
                        "  id_registro     BIGINT       NOT NULL," +
                        "  operacion       VARCHAR(10)  NOT NULL CHECK (operacion IN ('INSERT','UPDATE','DELETE'))," +
                        "  campo           VARCHAR(100) NOT NULL," +
                        "  valor_antes     TEXT," +
                        "  valor_despues   TEXT," +
                        "  usuario_bd      VARCHAR(100)," +
                        "  usuario_app     VARCHAR(255)," +
                        "  ip_cliente      VARCHAR(100)," +
                        "  hash_integridad VARCHAR(64)," +
                        "  fecha           TIMESTAMPTZ  NOT NULL DEFAULT NOW()" +
                        ")"
        );
        ejecutarSafe(
                "CREATE TABLE IF NOT EXISTS public.drive_config (" +
                        "  id            BIGSERIAL PRIMARY KEY," +
                        "  client_id     TEXT, client_secret TEXT, redirect_uri VARCHAR(500)," +
                        "  access_token  TEXT, refresh_token TEXT," +
                        "  token_type    VARCHAR(50) DEFAULT 'Bearer'," +
                        "  expires_at    TIMESTAMPTZ, scope TEXT," +
                        "  folder_id     TEXT, folder_name VARCHAR(255)," +
                        "  autorizado    BOOLEAN NOT NULL DEFAULT false," +
                        "  activo        BOOLEAN NOT NULL DEFAULT true," +
                        "  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
                        ")"
        );
        ejecutarSafe(
                "CREATE TABLE IF NOT EXISTS public.matriz_meritos_puntaje (" +
                        "  id_puntaje BIGSERIAL PRIMARY KEY," +
                        "  id_proceso BIGINT NOT NULL REFERENCES public.proceso_evaluacion(id_proceso)," +
                        "  item_id    VARCHAR(255) NOT NULL," +
                        "  valor      VARCHAR(255) NOT NULL," +
                        "  created_at TIMESTAMP DEFAULT NOW()" +
                        ")"
        );
    }

    // =========================================================
    // PERMISOS
    // =========================================================
    private void asignarPermisos() {
        ejecutarSafe("GRANT USAGE ON SCHEMA public TO readonly_user, base_app_permisos, role_admin_bd, role_lecturas, role_escritura, role_postulante_bd, role_evaluador_bd, role_revisor_bd, PUBLIC");
        ejecutarSafe("GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO role_admin_bd");
        ejecutarSafe("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO role_admin_bd");
        ejecutarSafe("GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_lecturas");
        ejecutarSafe("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO role_lecturas");
        ejecutarSafe("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO role_escritura");
        ejecutarSafe("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO role_escritura");
        ejecutarSafe("GRANT ALL ON ALL TABLES    IN SCHEMA public TO readonly_user");
        ejecutarSafe("GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO readonly_user");
        ejecutarSafe("GRANT role_escritura    TO readonly_user");
        ejecutarSafe("GRANT base_app_permisos TO readonly_user");
        ejecutarSafe("GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_evaluador_bd");
        ejecutarSafe("GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_revisor_bd");
        ejecutarSafe("GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_postulante_bd");
        ejecutarSafe("GRANT SELECT, INSERT, UPDATE ON sesiones_activas_app TO base_app_permisos");
        ejecutarSafe("GRANT SELECT ON v_sesiones_activas TO base_app_permisos");
        ejecutarSafe("GRANT SELECT, INSERT ON aud_cambio TO base_app_permisos");
        ejecutarSafe("GRANT SELECT ON config_backup, historial_backup TO base_app_permisos");
        ejecutarSafe("GRANT SELECT, INSERT, UPDATE ON notificacion TO base_app_permisos");
        ejecutarSafe("GRANT SELECT ON roles_app, roles_app_bd TO base_app_permisos");
        ejecutarSafe("GRANT INSERT, SELECT ON aud_login_app TO PUBLIC");
        ejecutarSafe("GRANT INSERT ON aud_cambio TO PUBLIC");
        ejecutarSafe("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO PUBLIC");
        ejecutarSafe("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO role_admin_bd, role_escritura, readonly_user");
        ejecutarSafe("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO role_admin_bd, role_escritura, readonly_user, PUBLIC");
    }

    // =========================================================
    // ROLES DE APLICACIÓN + INSTITUCIÓN BASE
    // =========================================================
    private void crearRolesApp() {
        ejecutarSafe(
                "INSERT INTO roles_app (nombre, descripcion, activo, fecha_creacion) VALUES " +
                        "('ADMIN',      'Administrador del sistema con acceso completo',   true, CURRENT_TIMESTAMP)," +
                        "('POSTULANTE', 'Usuario postulante al sistema',                   true, CURRENT_TIMESTAMP)," +
                        "('EVALUADOR',  'Usuario evaluador de postulaciones',              true, CURRENT_TIMESTAMP)," +
                        "('REVISOR',    'Revisor que aprueba postulaciones',               true, CURRENT_TIMESTAMP)" +
                        " ON CONFLICT (nombre) DO NOTHING"
        );
        ejecutarSafe(
                "INSERT INTO roles_app_bd (id_rol_app, nombre_rol_bd, fecha_creacion) " +
                        "SELECT id_rol_app, 'role_admin_bd', CURRENT_TIMESTAMP FROM roles_app WHERE nombre='ADMIN' ON CONFLICT DO NOTHING"
        );
        ejecutarSafe(
                "INSERT INTO roles_app_bd (id_rol_app, nombre_rol_bd, fecha_creacion) " +
                        "SELECT id_rol_app, unnest(ARRAY['role_postulante_bd','role_lecturas']), CURRENT_TIMESTAMP FROM roles_app WHERE nombre='POSTULANTE' ON CONFLICT DO NOTHING"
        );
        ejecutarSafe(
                "INSERT INTO roles_app_bd (id_rol_app, nombre_rol_bd, fecha_creacion) " +
                        "SELECT id_rol_app, unnest(ARRAY['role_evaluador_bd','role_lecturas']), CURRENT_TIMESTAMP FROM roles_app WHERE nombre='EVALUADOR' ON CONFLICT DO NOTHING"
        );
        ejecutarSafe(
                "INSERT INTO roles_app_bd (id_rol_app, nombre_rol_bd, fecha_creacion) " +
                        "SELECT id_rol_app, unnest(ARRAY['role_revisor_bd','role_lecturas']), CURRENT_TIMESTAMP FROM roles_app WHERE nombre='REVISOR' ON CONFLICT DO NOTHING"
        );
        ejecutarSafe(
                "INSERT INTO institucion (nombre, correo, direccion, telefono, activo, email_ssl, email_host, email_port) " +
                        "SELECT 'Universidad Técnica Estatal de Quevedo','info@uteq.edu.ec'," +
                        "'Av. Quito km 1.5 vía a Santo Domingo, Quevedo, Ecuador','05-3702220',true,false,'smtp.gmail.com',587 " +
                        "WHERE NOT EXISTS (SELECT 1 FROM institucion)"
        );
        ejecutarSafe(
                "INSERT INTO nivel_academico (nombre, orden, estado) VALUES " +
                        "('Pregrado',1,true),('Especialización',2,true),('Maestría',3,true)," +
                        "('Doctorado',4,true),('Postdoctorado',5,true) ON CONFLICT (nombre) DO NOTHING"
        );
    }

    // =========================================================
    // STORED PROCEDURES (CREATE OR REPLACE — siempre actualiza)
    // =========================================================
    private void crearStoredProcedures() {
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_registrar_usuario_simple(" +
                        "  p_usuario_app varchar, p_clave_app varchar, p_correo varchar," +
                        "  p_usuario_bd varchar, p_clave_bd_hash varchar, p_clave_bd_real varchar," +
                        "  p_roles_app varchar[]" +
                        ") RETURNS TABLE(out_id_usuario bigint, out_usuario_app varchar, out_usuario_bd varchar)" +
                        " LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$" +
                        " DECLARE v_id_usuario BIGINT; v_rol_app VARCHAR; v_id_rol_app INTEGER; v_rol_bd VARCHAR; v_roles_bd VARCHAR[];" +
                        " BEGIN" +
                        "   INSERT INTO usuario(usuario_app,clave_app,correo,usuario_bd,clave_bd,activo,fecha_creacion,primer_login,token_version)" +
                        "   VALUES(p_usuario_app,p_clave_app,p_correo,p_usuario_bd,p_clave_bd_hash,true,CURRENT_TIMESTAMP,true,1)" +
                        "   RETURNING usuario.id_usuario INTO v_id_usuario;" +
                        "   IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname=p_usuario_bd) THEN" +
                        "     EXECUTE format('CREATE USER %I WITH PASSWORD %L',p_usuario_bd,p_clave_bd_real);" +
                        "   END IF;" +
                        "   EXECUTE format('GRANT base_app_permisos TO %I',p_usuario_bd);" +
                        "   FOREACH v_rol_app IN ARRAY p_roles_app LOOP" +
                        "     SELECT ra.id_rol_app INTO v_id_rol_app FROM roles_app ra WHERE ra.nombre=v_rol_app AND ra.activo=true;" +
                        "     IF v_id_rol_app IS NULL THEN RAISE EXCEPTION 'Rol % no existe',v_rol_app; END IF;" +
                        "     SELECT ARRAY_AGG(rab.nombre_rol_bd) INTO v_roles_bd FROM roles_app_bd rab WHERE rab.id_rol_app=v_id_rol_app;" +
                        "     FOREACH v_rol_bd IN ARRAY v_roles_bd LOOP" +
                        "       EXECUTE format('GRANT %I TO %I',v_rol_bd,p_usuario_bd);" +
                        "     END LOOP;" +
                        "     INSERT INTO usuario_roles_app(id_usuario,id_rol_app) VALUES(v_id_usuario,v_id_rol_app) ON CONFLICT(id_usuario,id_rol_app) DO NOTHING;" +
                        "   END LOOP;" +
                        "   RETURN QUERY SELECT v_id_usuario,p_usuario_app::VARCHAR,p_usuario_bd::VARCHAR;" +
                        "   EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'Error en sp_registrar_usuario_simple: %',SQLERRM;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_login_validar(p_usuario_app varchar, p_clave_app varchar)" +
                        " RETURNS TABLE(usuario_bd varchar, clave_bd varchar, activo boolean, id_usuario bigint)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN RETURN QUERY SELECT u.usuario_bd::VARCHAR,u.clave_bd::VARCHAR,u.activo,u.id_usuario" +
                        " FROM usuario u WHERE u.usuario_app=p_usuario_app; END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_obtener_roles_app_usuario()" +
                        " RETURNS TABLE(rol_app varchar) LANGUAGE plpgsql SET search_path TO 'public' AS $fn$" +
                        " DECLARE v_roles_bd TEXT[];" +
                        " BEGIN" +
                        "   SELECT ARRAY_AGG(r.rolname) INTO v_roles_bd FROM pg_roles r" +
                        "   WHERE r.oid IN(SELECT roleid FROM pg_auth_members WHERE member=(SELECT oid FROM pg_roles WHERE rolname=current_user))" +
                        "   AND r.rolcanlogin=false AND r.rolname LIKE 'role_%';" +
                        "   RETURN QUERY SELECT DISTINCT ra.nombre::VARCHAR FROM roles_app ra" +
                        "   WHERE ra.activo=true AND EXISTS(SELECT 1 FROM roles_app_bd WHERE id_rol_app=ra.id_rol_app)" +
                        "   AND NOT EXISTS(SELECT 1 FROM roles_app_bd rab WHERE rab.id_rol_app=ra.id_rol_app AND NOT(rab.nombre_rol_bd=ANY(v_roles_bd)));" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_obtener_roles_app_usuario_con_id()" +
                        " RETURNS TABLE(id_rol_app integer, nombre varchar) LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN RETURN QUERY SELECT ra.id_rol_app,ra.nombre FROM usuario_roles_app ura" +
                        " JOIN roles_app ra ON ra.id_rol_app=ura.id_rol_app" +
                        " JOIN usuario u ON u.id_usuario=ura.id_usuario" +
                        " WHERE u.usuario_bd=current_user AND ra.activo=TRUE ORDER BY ra.id_rol_app; END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_obtener_roles_app_usuario_con_id(p_usuario_app varchar)" +
                        " RETURNS TABLE(id_rol_app integer, nombre varchar) LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN RETURN QUERY SELECT ra.id_rol_app,ra.nombre FROM usuario_roles_app ura" +
                        " JOIN roles_app ra ON ra.id_rol_app=ura.id_rol_app" +
                        " JOIN usuario u ON u.id_usuario=ura.id_usuario" +
                        " WHERE u.usuario_app=p_usuario_app AND ra.activo=TRUE ORDER BY ra.id_rol_app; END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_obtener_roles_app_usuario_por_nombre(p_usuario_bd varchar)" +
                        " RETURNS TABLE(nombre varchar) LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$" +
                        " DECLARE v_roles_bd TEXT[];" +
                        " BEGIN" +
                        "   SELECT ARRAY_AGG(r.rolname) INTO v_roles_bd FROM pg_roles r" +
                        "   WHERE r.oid IN(SELECT roleid FROM pg_auth_members WHERE member=(SELECT oid FROM pg_roles WHERE rolname=p_usuario_bd))" +
                        "   AND r.rolcanlogin=false AND r.rolname LIKE 'role_%';" +
                        "   RETURN QUERY SELECT DISTINCT ra.nombre::VARCHAR FROM roles_app ra" +
                        "   WHERE ra.activo=true AND EXISTS(SELECT 1 FROM roles_app_bd WHERE id_rol_app=ra.id_rol_app)" +
                        "   AND NOT EXISTS(SELECT 1 FROM roles_app_bd rab WHERE rab.id_rol_app=ra.id_rol_app" +
                        "   AND NOT(rab.nombre_rol_bd=ANY(COALESCE(v_roles_bd,ARRAY[]::TEXT[]))));" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_obtener_roles_bd_de_rol_app(p_nombre_rol_app varchar)" +
                        " RETURNS TABLE(rol_bd varchar) LANGUAGE plpgsql AS $fn$" +
                        " BEGIN RETURN QUERY SELECT rab.nombre_rol_bd::VARCHAR FROM roles_app_bd rab" +
                        " INNER JOIN roles_app ra ON rab.id_rol_app=ra.id_rol_app" +
                        " WHERE ra.nombre=p_nombre_rol_app ORDER BY rab.nombre_rol_bd; END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_registrar_sesion(p_usuario_app varchar, p_token_version integer, p_ip varchar DEFAULT NULL, p_user_agent text DEFAULT NULL)" +
                        " RETURNS bigint LANGUAGE plpgsql AS $fn$" +
                        " DECLARE v_id BIGINT;" +
                        " BEGIN" +
                        "   UPDATE public.sesiones_activas_app SET activo=FALSE,fecha_cierre=NOW(),motivo_cierre='NEW_LOGIN'" +
                        "   WHERE usuario_app=p_usuario_app AND activo=TRUE;" +
                        "   INSERT INTO public.sesiones_activas_app(usuario_app,token_version,ip_cliente,user_agent)" +
                        "   VALUES(p_usuario_app,p_token_version,p_ip,p_user_agent) RETURNING id_sesion INTO v_id;" +
                        "   RETURN v_id;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_obtener_opciones_usuario()" +
                        " RETURNS TABLE(modulo_nombre varchar, modulo_ruta varchar, opcion_id integer, opcion_nombre varchar," +
                        "               opcion_descripcion text, opcion_ruta varchar, opcion_orden integer, solo_lectura boolean)" +
                        " LANGUAGE plpgsql SECURITY DEFINER SET search_path TO 'public' AS $fn$" +
                        " DECLARE v_roles_bd TEXT[];" +
                        " BEGIN" +
                        "   SELECT array_agg(r.rolname) INTO v_roles_bd FROM pg_roles r" +
                        "   WHERE r.oid IN(SELECT roleid FROM pg_auth_members WHERE member=(SELECT oid FROM pg_roles WHERE rolname=current_user))" +
                        "   AND r.rolcanlogin=FALSE AND r.rolname LIKE 'role_%';" +
                        "   RETURN QUERY" +
                        "   SELECT m.nombre::VARCHAR,m.ruta::VARCHAR,o.id_opcion,o.nombre::VARCHAR,o.descripcion," +
                        "          o.ruta::VARCHAR,o.orden,rao.solo_lectura" +
                        "   FROM roles_app ra" +
                        "   JOIN modulo m ON m.id_modulo=ra.id_modulo" +
                        "   JOIN rol_app_opcion rao ON rao.id_rol_app=ra.id_rol_app" +
                        "   JOIN opcion o ON o.id_opcion=rao.id_opcion AND o.activo=TRUE" +
                        "   WHERE ra.activo=TRUE AND m.activo=TRUE" +
                        "   AND NOT EXISTS(SELECT 1 FROM roles_app_bd rab WHERE rab.id_rol_app=ra.id_rol_app AND NOT(rab.nombre_rol_bd=ANY(v_roles_bd)))" +
                        "   AND EXISTS(SELECT 1 FROM roles_app_bd WHERE id_rol_app=ra.id_rol_app)" +
                        "   ORDER BY m.orden,o.orden;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_obtener_opciones_usuario(p_id_rol_app integer)" +
                        " RETURNS TABLE(modulo_nombre varchar, modulo_ruta varchar, opcion_id integer, opcion_nombre varchar," +
                        "               opcion_descripcion text, opcion_ruta varchar, opcion_orden integer, solo_lectura boolean)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN" +
                        "   RETURN QUERY SELECT DISTINCT m.nombre::VARCHAR,m.ruta::VARCHAR,o.id_opcion,o.nombre::VARCHAR," +
                        "          o.descripcion,o.ruta::VARCHAR,o.orden,rao.solo_lectura" +
                        "   FROM rol_app_opcion rao" +
                        "   JOIN opcion o ON o.id_opcion=rao.id_opcion" +
                        "   JOIN modulo m ON m.id_modulo=o.id_modulo" +
                        "   WHERE rao.id_rol_app=p_id_rol_app AND o.activo=TRUE ORDER BY o.orden ASC;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_notificaciones_usuario(p_id_usuario bigint, p_solo_no_leidas boolean DEFAULT false, p_limite integer DEFAULT 30)" +
                        " RETURNS TABLE(id_notificacion bigint, tipo varchar, titulo varchar, mensaje text, leida boolean," +
                        "               entidad_tipo varchar, entidad_id bigint, fecha_creacion timestamp without time zone, tiempo_relativo text)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN RETURN QUERY" +
                        "   SELECT n.id_notificacion,n.tipo,n.titulo,n.mensaje,n.leida,n.entidad_tipo,n.entidad_id,n.fecha_creacion," +
                        "     CASE" +
                        "       WHEN EXTRACT(EPOCH FROM(NOW()-n.fecha_creacion))<60     THEN 'Ahora mismo'" +
                        "       WHEN EXTRACT(EPOCH FROM(NOW()-n.fecha_creacion))<3600   THEN 'Hace '||FLOOR(EXTRACT(EPOCH FROM(NOW()-n.fecha_creacion))/60)::TEXT||' min'" +
                        "       WHEN EXTRACT(EPOCH FROM(NOW()-n.fecha_creacion))<86400  THEN 'Hace '||FLOOR(EXTRACT(EPOCH FROM(NOW()-n.fecha_creacion))/3600)::TEXT||' hora(s)'" +
                        "       WHEN EXTRACT(EPOCH FROM(NOW()-n.fecha_creacion))<604800 THEN 'Hace '||FLOOR(EXTRACT(EPOCH FROM(NOW()-n.fecha_creacion))/86400)::TEXT||' día(s)'" +
                        "       ELSE TO_CHAR(n.fecha_creacion,'DD/MM/YYYY') END" +
                        "   FROM notificacion n WHERE n.id_usuario=p_id_usuario AND(NOT p_solo_no_leidas OR n.leida=FALSE)" +
                        "   ORDER BY n.fecha_creacion DESC LIMIT p_limite;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_contar_no_leidas(p_id_usuario bigint)" +
                        " RETURNS integer LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " DECLARE v_total INTEGER;" +
                        " BEGIN SELECT COUNT(*) INTO v_total FROM notificacion WHERE id_usuario=p_id_usuario AND leida=FALSE;" +
                        " RETURN COALESCE(v_total,0); END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_listar_roles_app()" +
                        " RETURNS TABLE(id_rol_app integer, nombre varchar, descripcion text, activo boolean)" +
                        " LANGUAGE plpgsql AS $fn$" +
                        " BEGIN RETURN QUERY SELECT ra.id_rol_app,ra.nombre::VARCHAR,ra.descripcion,ra.activo" +
                        " FROM roles_app ra ORDER BY ra.nombre; END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_listar_roles_bd_disponibles()" +
                        " RETURNS TABLE(rol_nombre varchar, descripcion text) LANGUAGE plpgsql AS $fn$" +
                        " BEGIN RETURN QUERY SELECT r.rolname::VARCHAR,COALESCE(d.description,'Sin descripción')::TEXT" +
                        " FROM pg_roles r LEFT JOIN pg_description d ON d.objoid=r.oid AND d.classoid='pg_authid'::regclass" +
                        " WHERE r.rolcanlogin=false AND r.rolname LIKE 'role_%' ORDER BY r.rolname; END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.fn_auditar_login_app(" +
                        "  p_usuario_app text, p_usuario_bd text, p_resultado text," +
                        "  p_motivo text DEFAULT NULL, p_ip_cliente inet DEFAULT NULL," +
                        "  p_user_agent text DEFAULT NULL, p_id_usuario bigint DEFAULT NULL)" +
                        " RETURNS void LANGUAGE plpgsql AS $fn$" +
                        " BEGIN INSERT INTO public.aud_login_app(fecha,usuario_app,usuario_bd,resultado,motivo,ip_cliente,user_agent,id_usuario)" +
                        " VALUES(CURRENT_TIMESTAMP,p_usuario_app,p_usuario_bd,p_resultado,p_motivo,p_ip_cliente,p_user_agent,p_id_usuario); END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE PROCEDURE public.sp_post_restauracion() LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " DECLARE v_usuario_bd VARCHAR; v_rol_bd VARCHAR;" +
                        " BEGIN" +
                        "   BEGIN CREATE ROLE base_app_permisos  NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END;" +
                        "   BEGIN CREATE ROLE role_admin_bd       NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END;" +
                        "   BEGIN CREATE ROLE role_lecturas       NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END;" +
                        "   BEGIN CREATE ROLE role_escritura      NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END;" +
                        "   BEGIN CREATE ROLE role_postulante_bd  NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END;" +
                        "   BEGIN CREATE ROLE role_evaluador_bd   NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END;" +
                        "   BEGIN CREATE ROLE role_revisor_bd     NOLOGIN; EXCEPTION WHEN duplicate_object THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO role_admin_bd'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO role_admin_bd'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_lecturas'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA public TO role_escritura'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_evaluador_bd'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_revisor_bd'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT SELECT ON ALL TABLES IN SCHEMA public TO role_postulante_bd'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT ALL ON ALL TABLES IN SCHEMA public TO readonly_user'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO readonly_user'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT role_escritura TO readonly_user'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   BEGIN EXECUTE 'GRANT base_app_permisos TO readonly_user'; EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   FOR v_usuario_bd,v_rol_bd IN" +
                        "     SELECT DISTINCT u.usuario_bd,rab.nombre_rol_bd FROM usuario u" +
                        "     JOIN usuario_roles_app ura ON ura.id_usuario=u.id_usuario" +
                        "     JOIN roles_app_bd rab ON rab.id_rol_app=ura.id_rol_app" +
                        "     WHERE u.usuario_bd IS NOT NULL AND u.usuario_bd NOT IN('postgres','readonly_user','pgbouncer')" +
                        "     AND EXISTS(SELECT 1 FROM pg_roles WHERE rolname=u.usuario_bd)" +
                        "   LOOP" +
                        "     BEGIN EXECUTE format('GRANT base_app_permisos TO %I',v_usuario_bd); EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "     BEGIN EXECUTE format('GRANT %I TO %I',v_rol_bd,v_usuario_bd); EXCEPTION WHEN OTHERS THEN NULL; END;" +
                        "   END LOOP;" +
                        "   RAISE NOTICE 'sp_post_restauracion ejecutado correctamente';" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE PROCEDURE public.sp_primer_login_cambiar_clave_app(p_usuario_app varchar, p_clave_app varchar)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN" +
                        "   UPDATE usuario SET clave_app=p_clave_app, primer_login=false WHERE usuario_app=p_usuario_app;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE PROCEDURE public.sp_cambiar_clave_app(p_usuario_app varchar, p_clave_app varchar)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN" +
                        "   UPDATE usuario SET clave_app=p_clave_app WHERE usuario_app=p_usuario_app;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE PROCEDURE public.sp_recuperar_clave_app(p_usuario_app varchar, p_clave_app varchar)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN" +
                        "   UPDATE usuario SET clave_app=p_clave_app, primer_login=true WHERE usuario_app=p_usuario_app;" +
                        " END; $fn$"
        );
        // ── TIPO DOCUMENTO ──────────────────────────────────────
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_listar_tipos_documento()" +
                        " RETURNS TABLE(id_tipo_documento bigint, nombre varchar, descripcion text, obligatorio boolean, activo boolean)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN RETURN QUERY SELECT t.id_tipo_documento,t.nombre::VARCHAR,t.descripcion::text,t.obligatorio,t.activo" +
                        " FROM tipo_documento t ORDER BY t.nombre; END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_crear_tipo_documento(p_nombre varchar, p_descripcion text, p_obligatorio boolean)" +
                        " RETURNS TABLE(v_id bigint, v_mensaje varchar)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " DECLARE v_new_id BIGINT;" +
                        " BEGIN" +
                        "   IF EXISTS(SELECT 1 FROM tipo_documento WHERE nombre=p_nombre) THEN" +
                        "     RETURN QUERY SELECT NULL::BIGINT,'Ya existe un tipo de documento con ese nombre'::VARCHAR;" +
                        "     RETURN;" +
                        "   END IF;" +
                        "   INSERT INTO tipo_documento(nombre,descripcion,obligatorio,activo) VALUES(p_nombre,p_descripcion,p_obligatorio,true) RETURNING id_tipo_documento INTO v_new_id;" +
                        "   RETURN QUERY SELECT v_new_id,'Tipo de documento creado correctamente'::VARCHAR;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_editar_tipo_documento(p_id bigint, p_nombre varchar, p_descripcion text, p_obligatorio boolean)" +
                        " RETURNS TABLE(v_exitoso boolean, v_mensaje varchar)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " BEGIN" +
                        "   IF NOT EXISTS(SELECT 1 FROM tipo_documento WHERE id_tipo_documento=p_id) THEN" +
                        "     RETURN QUERY SELECT false,'Tipo de documento no encontrado'::VARCHAR; RETURN;" +
                        "   END IF;" +
                        "   IF EXISTS(SELECT 1 FROM tipo_documento WHERE nombre=p_nombre AND id_tipo_documento<>p_id) THEN" +
                        "     RETURN QUERY SELECT false,'Ya existe un tipo de documento con ese nombre'::VARCHAR; RETURN;" +
                        "   END IF;" +
                        "   UPDATE tipo_documento SET nombre=p_nombre,descripcion=p_descripcion,obligatorio=p_obligatorio WHERE id_tipo_documento=p_id;" +
                        "   RETURN QUERY SELECT true,'Tipo de documento actualizado correctamente'::VARCHAR;" +
                        " END; $fn$"
        );
        ejecutarSafe(
                "CREATE OR REPLACE FUNCTION public.sp_toggle_tipo_documento(p_id bigint)" +
                        " RETURNS TABLE(v_exitoso boolean, v_activo boolean, v_mensaje varchar)" +
                        " LANGUAGE plpgsql SECURITY DEFINER AS $fn$" +
                        " DECLARE v_nuevo boolean;" +
                        " BEGIN" +
                        "   IF NOT EXISTS(SELECT 1 FROM tipo_documento WHERE id_tipo_documento=p_id) THEN" +
                        "     RETURN QUERY SELECT false,false,'Tipo de documento no encontrado'::VARCHAR; RETURN;" +
                        "   END IF;" +
                        "   UPDATE tipo_documento SET activo=NOT activo WHERE id_tipo_documento=p_id RETURNING activo INTO v_nuevo;" +
                        "   RETURN QUERY SELECT true,v_nuevo,'Estado actualizado correctamente'::VARCHAR;" +
                        " END; $fn$"
        );
        // ── PERMISOS COMPLETOS PARA role_admin_bd ───────────────
        ejecutarSafe("GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO role_admin_bd");
        ejecutarSafe("GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO role_admin_bd");
        ejecutarSafe("GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO role_admin_bd");
        ejecutarSafe("GRANT ALL PRIVILEGES ON ALL PROCEDURES IN SCHEMA public TO role_admin_bd");
        ejecutarSafe("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES    TO role_admin_bd");
        ejecutarSafe("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO role_admin_bd");
        ejecutarSafe("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON FUNCTIONS TO role_admin_bd");
        log.info("  [+] Stored procedures actualizados.");
    }

    // =========================================================
    // LLAMADO DESDE ModuloDataInitializer
    // =========================================================
    public void asignarOpcionesAdmin() {
        try {
            Integer count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM rol_app_opcion rao " +
                            "JOIN roles_app ra ON ra.id_rol_app=rao.id_rol_app " +
                            "JOIN opcion o ON o.id_opcion=rao.id_opcion " +
                            "JOIN modulo m ON m.id_modulo=o.id_modulo " +
                            "WHERE ra.nombre='ADMIN' AND m.nombre='admin'",
                    Integer.class
            );
            if (count != null && count > 0) {
                log.info("  [+] ADMIN ya tiene {} opciones asignadas, skip.", count);
                limpiarHuerfanos();
                return;
            }
        } catch (Exception e) {
            log.debug("  No se pudo verificar opciones del ADMIN: {}", e.getMessage());
        }
        log.info("  [+] Asignando opciones del módulo admin al rol ADMIN...");
        ejecutarSafe(
                "UPDATE roles_app SET id_modulo=(SELECT id_modulo FROM modulo WHERE nombre='admin') " +
                        "WHERE nombre='ADMIN' AND id_modulo IS NULL"
        );
        ejecutarSafe(
                "INSERT INTO rol_app_opcion(id_rol_app,id_opcion,solo_lectura) " +
                        "SELECT ra.id_rol_app,o.id_opcion,false " +
                        "FROM roles_app ra " +
                        "JOIN modulo m ON m.nombre='admin' " +
                        "JOIN opcion o ON o.id_modulo=m.id_modulo " +
                        "WHERE ra.nombre='ADMIN' AND o.activo=true " +
                        "ON CONFLICT(id_opcion,id_rol_app) DO NOTHING"
        );
        limpiarHuerfanos();
        log.info("  [+] Opciones del ADMIN asignadas.");
    }

    private void limpiarHuerfanos() {
        ejecutarSafe("DELETE FROM autoridad_academica WHERE id_usuario NOT IN(SELECT id_usuario FROM usuario)");
        ejecutarSafe("DELETE FROM postulante         WHERE id_usuario  NOT IN(SELECT id_usuario FROM usuario)");
        ejecutarSafe("DELETE FROM postulacion         WHERE id_postulante NOT IN(SELECT id_postulante FROM postulante)");
        ejecutarSafe("DELETE FROM proceso_evaluacion  WHERE id_postulante NOT IN(SELECT id_postulante FROM postulante)");
    }

    private void ejecutarSafe(String sql) {
        try { jdbc.execute(sql); }
        catch (Exception e) { log.debug("  SQL ignorado: {}", e.getMessage()); }
    }

    // =========================================================
    // SCRIPT SQL DE LÓGICA DE NEGOCIO
    // =========================================================
    private void ejecutarScriptSql() {
        try {
            ClassPathResource resource = new ClassPathResource("db/sp_negocio.sql");
            if (!resource.exists()) {
                log.warn("  [!] db/sp_negocio.sql no encontrado, saltando.");
                return;
            }
            String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<String> statements = splitSqlRespetandoDollarQuote(sql);
            int ok = 0;
            for (String stmt : statements) {
                String trimmed = stmt.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    try {
                        jdbc.execute(trimmed);
                        ok++;
                    } catch (Exception e) {
                        log.debug("  SQL negocio ignorado: {}", e.getMessage());
                    }
                }
            }
            log.info("  [+] sp_negocio.sql ejecutado ({} statements).", ok);
        } catch (Exception e) {
            log.warn("  [!] Error leyendo sp_negocio.sql: {}", e.getMessage());
        }
    }

    /**
     * Divide un script SQL en statements individuales respetando el dollar-quoting
     * de PostgreSQL ($fn$...$fn$, $proc$...$proc$, $$...$$, etc.).
     * Los semicolons DENTRO de un bloque dollar-quoted son ignorados como separadores.
     */
    private List<String> splitSqlRespetandoDollarQuote(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String dollarTag = null;
        int i = 0;
        int len = sql.length();

        while (i < len) {
            char c = sql.charAt(i);

            if (dollarTag == null && c == '$') {
                // Intento leer un dollar-tag: $identificador$
                int j = i + 1;
                while (j < len && (Character.isLetterOrDigit(sql.charAt(j)) || sql.charAt(j) == '_')) {
                    j++;
                }
                if (j < len && sql.charAt(j) == '$') {
                    // Encontramos un dollar-tag válido
                    dollarTag = sql.substring(i, j + 1);
                    current.append(dollarTag);
                    i = j + 1;
                    continue;
                }
            } else if (dollarTag != null && sql.startsWith(dollarTag, i)) {
                // Cerramos el bloque dollar-quoted actual
                current.append(dollarTag);
                i += dollarTag.length();
                dollarTag = null;
                continue;
            }

            if (dollarTag == null && c == ';') {
                // Semicolon fuera de dollar-quote → fin de statement
                current.append(c);
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                current = new StringBuilder();
            } else {
                current.append(c);
            }
            i++;
        }

        // Cualquier texto restante (sin semicolon final)
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }
        return statements;
    }

}