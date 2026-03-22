package org.uteq.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class ModuloDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ModuloDataInitializer.class);
    private final JdbcTemplate jdbc;
    private final DatabaseSetupInitializer dbSetup;

    public ModuloDataInitializer(JdbcTemplate jdbc, DatabaseSetupInitializer dbSetup) {
        this.jdbc    = jdbc;
        this.dbSetup = dbSetup;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(">>> [ModuloDataInitializer] Verificando módulos y opciones...");
        insertarTodo();
        dbSetup.asignarOpcionesAdmin();
        log.info(">>> [ModuloDataInitializer] Listo.");
    }

    private void insertarTodo() {
        insertarModulos();
        insertarOpcionesAdmin();
        insertarOpcionesEvaluador();
        insertarOpcionesRevisor();
        insertarOpcionesPostulante();
    }

    // =========================================================
    // MÓDULOS
    // =========================================================
    private void insertarModulos() {
        ejecutarSafe(
                "INSERT INTO modulo(nombre,descripcion,ruta,orden,activo,fecha_creacion) VALUES" +
                        "('admin',      'Panel de Administrador', '/admin',      1, true, CURRENT_TIMESTAMP)," +
                        "('evaluador',  'Panel de evaluador',     '/evaluador',  2, true, CURRENT_TIMESTAMP)," +
                        "('revisor',    'Panel de Revisor',        '/revisor',    3, true, CURRENT_TIMESTAMP)," +
                        "('postulante', 'Panel de postulante',     '/postulante', 4, true, CURRENT_TIMESTAMP)" +
                        " ON CONFLICT(nombre) DO NOTHING"
        );
    }

    // =========================================================
    // OPCIONES — ADMIN (13 opciones originales)
    // =========================================================
    private void insertarOpcionesAdmin() {
        ejecutarSafe(
                "INSERT INTO opcion(id_modulo,nombre,descripcion,ruta,orden,activo,fecha_creacion)" +
                        " SELECT m.id_modulo, v.nombre, v.descripcion, v.ruta, v.orden, true, CURRENT_TIMESTAMP" +
                        " FROM modulo m," +
                        " (VALUES" +
                        "   ('Usuarios',                  'Gestión de usuarios',                          '/gestion-usuarios',    1)," +
                        "   ('Roles',                     'Gestión de roles',                             '/gestion-roles',       2)," +
                        "   ('Gestión Documentos',        'Administrar documentos',                       '/gestion-documentos',  3)," +
                        "   ('Facultades',               'Gestión académica',                             '/facultad',            4)," +
                        "   ('Carreras',                 'Gestión de carreras',                           '/carrera',             5)," +
                        "   ('Materias',                 'Gestión de materias',                           '/materia',             6)," +
                        "   ('Áreas de Conocimiento',    'Gestión de áreas académicas',                  '/area-conocimiento',   7)," +
                        "   ('Postulantes',              'Información de postulantes',                    '/gestion-postulante',  8)," +
                        "   ('Auditoría',               'Registro de sesiones',                           '/auditoria',           9)," +
                        "   ('Gestión Opciones',         'Configurar módulos y opciones',                 '/gestion-opciones',   10)," +
                        "   ('Configuración institucional','Configurar datos de la institución',          '/config-institucion', 11)," +
                        "   ('Niveles Académicos',       'Configurar niveles académicos',                 '/niveles-academicos', 12)," +
                        "   ('Respaldos',               'Configurar y ver historial de respaldos',        '/backup',             13)" +
                        " ) AS v(nombre,descripcion,ruta,orden)" +
                        " WHERE m.nombre='admin'" +
                        " ON CONFLICT DO NOTHING"
        );
    }

    // =========================================================
    // OPCIONES — EVALUADOR
    // =========================================================
    private void insertarOpcionesEvaluador() {
        ejecutarSafe(
                "INSERT INTO opcion(id_modulo,nombre,descripcion,ruta,orden,activo,fecha_creacion)" +
                        " SELECT m.id_modulo, v.nombre, v.descripcion, v.ruta, v.orden, true, CURRENT_TIMESTAMP" +
                        " FROM modulo m," +
                        " (VALUES" +
                        "   ('Postulantes',         'Registro y seguimiento',              '/evaluador/postulantes',          1)," +
                        "   ('Documentos',          'Gestión de archivos',                 '/evaluador/documentos',           2)," +
                        "   ('Matriz de méritos',   'Matriz de méritos docente',           '/evaluador/matriz-meritos',       3)," +
                        "   ('Reportes',            'Informes en PDF',                     '/evaluador/reportes',             4)," +
                        "   ('Solicitar Docente',   'Nueva requisición',                   '/evaluador/solicitar',            5)," +
                        "   ('Entrevistas',         'Gestión de entrevistas',              '/evaluador/entrevista',           6)," +
                        "   ('Entrevistas Docentes','Proceso de evaluación docente',       '/evaluador/entrevistas-docentes', 7)" +
                        " ) AS v(nombre,descripcion,ruta,orden)" +
                        " WHERE m.nombre='evaluador'" +
                        " ON CONFLICT DO NOTHING"
        );
    }

    // =========================================================
    // OPCIONES — REVISOR
    // =========================================================
    private void insertarOpcionesRevisor() {
        ejecutarSafe(
                "INSERT INTO opcion(id_modulo,nombre,descripcion,ruta,orden,activo,fecha_creacion)" +
                        " SELECT m.id_modulo, v.nombre, v.descripcion, v.ruta, v.orden, true, CURRENT_TIMESTAMP" +
                        " FROM modulo m," +
                        " (VALUES" +
                        "   ('Pre-postulaciones',    'Revisar postulaciones',                               '/revisor/prepostulaciones',   1)," +
                        "   ('Convocatorias',        'Gestionar convocatorias',                             '/revisor/convocatorias',      2)," +
                        "   ('Solicitudes Docente',  'Gestionar solicitudes de contratación docente',       '/revisor/solicitudes-docente',3)," +
                        "   ('Configurar Matriz',    'Configurar matriz de méritos',                        '/revisor/config-matriz',      4)," +
                        "   ('Configurar Entrevistas','Configuración de fases, plantillas y criterios',     '/revisor/config-entrevistas', 5)," +
                        "   ('Decisiones del Comité','Decisiones finales del comité evaluador',             '/revisor/decisiones',         6)" +
                        " ) AS v(nombre,descripcion,ruta,orden)" +
                        " WHERE m.nombre='revisor'" +
                        " ON CONFLICT DO NOTHING"
        );
    }

    // =========================================================
    // OPCIONES — POSTULANTE
    // =========================================================
    private void insertarOpcionesPostulante() {
        ejecutarSafe(
                "INSERT INTO opcion(id_modulo,nombre,descripcion,ruta,orden,activo,fecha_creacion)" +
                        " SELECT m.id_modulo, v.nombre, v.descripcion, v.ruta, v.orden, true, CURRENT_TIMESTAMP" +
                        " FROM modulo m," +
                        " (VALUES" +
                        "   ('Mis Documentos','Subir y gestionar docs',   '/postulante/subir-documentos', 1)," +
                        "   ('Resultados',    'Ver resultados',           '/postulante/resultados',        2)," +
                        "   ('Entrevistas',   'Información entrevistas',  '/postulante/entrevista',        3)" +
                        " ) AS v(nombre,descripcion,ruta,orden)" +
                        " WHERE m.nombre='postulante'" +
                        " ON CONFLICT DO NOTHING"
        );
    }

    private void ejecutarSafe(String sql) {
        try { jdbc.execute(sql); }
        catch (Exception e) { log.debug("  SQL ignorado: {}", e.getMessage()); }
    }
}