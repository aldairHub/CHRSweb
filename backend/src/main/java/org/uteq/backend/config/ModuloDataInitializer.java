package org.uteq.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.entity.Modulo;
import org.uteq.backend.entity.Opcion;
import org.uteq.backend.repository.ModuloRepository;
import org.uteq.backend.repository.OpcionRepository;

@Component
public class ModuloDataInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(ModuloDataInitializer.class);

    private final ModuloRepository moduloRepository;
    private final OpcionRepository opcionRepository;

    public ModuloDataInitializer(ModuloRepository moduloRepository,
                                 OpcionRepository opcionRepository) {
        this.moduloRepository = moduloRepository;
        this.opcionRepository  = opcionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("ModuloDataInitializer — verificando datos iniciales...");
        insertarTodo();
        log.info("ModuloDataInitializer — listo.");
    }

    private void insertarTodo() {
        // ── ADMIN ───────────────────────────────────────────────────
        Modulo adm = mod("admin","Panel de Administrador","/admin",1);
        op(adm,"Usuarios",          "Gestión de usuarios",          "/gestion-usuarios",   1);
        op(adm,"Roles",             "Gestión de roles",             "/gestion-roles",      2);
        op(adm,"Gestión Documentos","Administrar documentos",       "/gestion-documentos", 3);
        op(adm,"Facultades",        "Gestión académica",            "/facultad",           4);
        op(adm,"Carreras",          "Gestión de carreras",          "/carrera",            5);
        op(adm,"Materias",          "Gestión de materias",          "/materia",            6);
        op(adm,"Postulantes",       "Información de postulantes",   "/gestion-postulante", 7);
        op(adm,"Auditoría",         "Registro de sesiones",         "/auditoria",          8);
        op(adm,"Gestión Opciones",  "Configurar módulos y opciones","/gestion-opciones",   9);
        // ── EVALUADOR ───────────────────────────────────────────────
        Modulo ev = mod("evaluador","Panel de Evaluador","/evaluador",2);
        op(ev,"Postulantes",       "Registro y seguimiento",  "/evaluador/postulantes",1);
        op(ev,"Documentos",        "Gestión de archivos",     "/evaluador/documentos", 2);
        op(ev,"Evaluación",        "Matriz de méritos",       "/evaluador/evaluacion", 3);
        op(ev,"Reportes",          "Informes en PDF",         "/evaluador/reportes",   4);
        op(ev,"Solicitar Docente", "Nueva requisición",       "/evaluador/solicitar",  5);
        op(ev,"Entrevistas",       "Gestión de entrevistas",  "/evaluador/entrevistas",6);
        // ── REVISOR ─────────────────────────────────────────────────
        Modulo rev = mod("revisor","Panel de Revisor","/revisor",3);
        op(rev,"Pre-postulaciones","Revisar postulaciones",   "/revisor/prepostulaciones",1);
        op(rev,"Convocatorias",    "Gestionar convocatorias", "/revisor/convocatorias",   2);
        // ── POSTULANTE ──────────────────────────────────────────────
        Modulo pos = mod("postulante","Panel de Postulante","/postulante",4);
        op(pos,"Mis Documentos","Subir y gestionar docs",  "/postulante/subir-documentos",1);
        op(pos,"Resultados",    "Ver resultados",          "/postulante/resultados",      2);
        op(pos,"Entrevistas",   "Información entrevistas", "/postulante/entrevistas",     3);
    }

    private Modulo mod(String nombre, String desc, String ruta, int orden) {
        return moduloRepository.findByNombre(nombre).orElseGet(() -> {
            Modulo m = new Modulo();
            m.setNombre(nombre); m.setDescripcion(desc);
            m.setRuta(ruta); m.setOrden(orden); m.setActivo(true);
            log.info("Creando módulo: {}", nombre);
            return moduloRepository.save(m);
        });
    }

    private void op(Modulo modulo, String nombre, String desc,
                    String ruta, int orden) {
        if (opcionRepository.existsByModulo_IdModuloAndRuta(
                modulo.getIdModulo(), ruta)) return;
        Opcion o = new Opcion();
        o.setModulo(modulo); o.setNombre(nombre); o.setDescripcion(desc);
        o.setRuta(ruta); o.setOrden(orden); o.setActivo(true);
        opcionRepository.save(o);
        log.info("  opción: {} ({})", nombre, ruta);
    }
}
