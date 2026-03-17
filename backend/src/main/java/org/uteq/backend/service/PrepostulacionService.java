package org.uteq.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.dto.RegistroSpResultDTO;
import org.uteq.backend.entity.Postulante;
import org.uteq.backend.entity.Prepostulacion;
import org.uteq.backend.repository.*;
import org.uteq.backend.dto.PrepostulacionResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrepostulacionService {
    private static final Logger log = LoggerFactory.getLogger(PrepostulacionService.class);

    private final NotificacionService notifService;
    private final PrepostulacionRepository prepostulacionRepository;
    private final SupabaseStorageService supabaseService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RolAppRepository rolAppRepository;
    private final PostgresProcedureRepository postgresProcedureRepository;
    private final AesCipherService aesCipherService;
    private final PrepostulacionDocumentoRepository documentoRepository;
    private final PostulanteRepository postulanteRepository;
    private final PrepostulacionSolicitudRepository prepostulacionSolicitudRepository;

    public PrepostulacionService(
            NotificacionService notifService,
            PrepostulacionRepository prepostulacionRepository,
            SupabaseStorageService supabaseService,
            UsuarioRepository usuarioRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            RolAppRepository rolAppRepository,
            PostgresProcedureRepository postgresProcedureRepository,
            AesCipherService aesCipherService,
            PrepostulacionDocumentoRepository documentoRepository,
            PostulanteRepository postulanteRepository,
            PrepostulacionSolicitudRepository prepostulacionSolicitudRepository
    ) {
        this.notifService = notifService;
        this.prepostulacionRepository = prepostulacionRepository;
        this.supabaseService = supabaseService;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rolAppRepository = rolAppRepository;
        this.postgresProcedureRepository = postgresProcedureRepository;
        this.aesCipherService = aesCipherService;
        this.documentoRepository = documentoRepository;
        this.postulanteRepository = postulanteRepository;
        this.prepostulacionSolicitudRepository = prepostulacionSolicitudRepository;
    }

    // =========================================================================
    // REGISTRO INICIAL
    // =========================================================================

    @Transactional
    public PrepostulacionResponseDTO procesarPrepostulacion(
            String correo,
            String cedula,
            String nombres,
            String apellidos,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            List<MultipartFile> archivosDocumentos,
            List<String> descripcionesDocumentos,
            Long idSolicitud
    ) {
        log.info("Procesando prepostulacion para identificacion: {}", cedula);

        String urlCedula, urlFoto;
        try {
            log.debug("Subiendo cedula a Supabase...");
            urlCedula = supabaseService.subirArchivo(archivoCedula, "cedulas", cedula);
            log.debug("Subiendo foto a Supabase...");
            urlFoto = supabaseService.subirArchivo(archivoFoto, "fotos", cedula);
            log.info("Archivos base de prepostulacion subidos exitosamente");
        } catch (Exception e) {
            log.error("Error al subir archivos base: {}", e.getMessage());
            throw new RuntimeException("Error al subir archivos: " + e.getMessage());
        }

        Long idPrepostulacion = postgresProcedureRepository.registrarPrepostulacion(
                nombres, apellidos, cedula, correo, urlCedula, urlFoto, idSolicitud);

        log.info("Prepostulacion guardada con ID: {}", idPrepostulacion);
        if (idSolicitud != null) {
            log.info("Prepostulacion {} vinculada a solicitud {}", idPrepostulacion, idSolicitud);
        }

        String tag = cedula + "_" + System.currentTimeMillis();
        for (int i = 0; i < archivosDocumentos.size(); i++) {
            MultipartFile archivo = archivosDocumentos.get(i);
            String descripcion = (descripcionesDocumentos != null && i < descripcionesDocumentos.size())
                    ? descripcionesDocumentos.get(i)
                    : "Documento " + (i + 1);
            try {
                log.debug("Subiendo documento academico {} de {}: {}", i + 1, archivosDocumentos.size(), descripcion);
                String urlDoc = supabaseService.subirArchivo(archivo, "documentos_academicos", tag + "_" + i);
                postgresProcedureRepository.agregarDocumentoPrepostulacion(idPrepostulacion, descripcion, urlDoc);
                log.debug("Documento {} registrado correctamente", i + 1);
            } catch (Exception e) {
                log.error("Error al subir documento academico {}: {}", i + 1, e.getMessage());
                throw new RuntimeException("Error al subir documento académico " + (i + 1) + ": " + e.getMessage());
            }
        }

        log.info("Prepostulacion {} completada con {} documento(s) academico(s)",
                idPrepostulacion, archivosDocumentos.size());

        notifService.notifRevisorNuevaPrepostulacion(idPrepostulacion, nombres + " " + apellidos);

        return new PrepostulacionResponseDTO(
                "Solicitud registrada exitosamente", correo, idPrepostulacion, true, LocalDateTime.now());
    }

    // =========================================================================
    // REPOSTULACIÓN  (lógica nueva — igual que prepostulación inicial)
    // =========================================================================

    /**
     * Re-postulación adaptada a la nueva lógica de documentos dinámicos.
     *
     * Flujo:
     *   1. Subir cédula y foto a Supabase.
     *   2. Llamar sp_repostular(cedula, idSolicitud, urlCedula, urlFoto)
     *      → crea una NUEVA fila en prepostulacion con estado PENDIENTE.
     *   3. Para cada documento académico, subir a Supabase y registrar
     *      con sp_agregar_documento_prepostulacion(idNuevaPrepostulacion, descripcion, urlDoc).
     *   4. Notificar al revisor.
     */
    @Transactional
    public PrepostulacionResponseDTO repostular(
            String cedula,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            List<MultipartFile> archivosDocumentos,
            List<String> descripcionesDocumentos,
            Long idSolicitud
    ) {
        // ── 1. Validaciones básicas ──────────────────────────────────────────
        if (archivosDocumentos == null || archivosDocumentos.isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un documento académico.");
        }

        // ── 2. Subir archivos base ───────────────────────────────────────────
        String urlCedula, urlFoto;
        try {
            String tag = cedula + "_repost_" + System.currentTimeMillis();
            urlCedula = supabaseService.subirArchivo(archivoCedula, "cedulas", tag);
            urlFoto   = supabaseService.subirArchivo(archivoFoto,   "fotos",   tag);
            log.info("Archivos base de re-postulacion subidos para cedula: {}", cedula);
        } catch (Exception e) {
            throw new RuntimeException("Error al subir documentos: " + e.getMessage());
        }

        // ── 3. Crear nueva fila de prepostulacion (sp_repostular 4 args) ────
        Long idNuevaPrepostulacion;
        try {
            idNuevaPrepostulacion = postgresProcedureRepository.repostular(
                    cedula, idSolicitud, urlCedula, urlFoto);
            log.info("Re-postulacion guardada como nueva fila con ID: {}", idNuevaPrepostulacion);
        } catch (Exception e) {
            throw new RuntimeException(extraerMensajeTrigger(e));
        }

        // ── 4. Subir y registrar documentos académicos dinámicos ─────────────
        String tag = cedula + "_repost_" + System.currentTimeMillis();
        for (int i = 0; i < archivosDocumentos.size(); i++) {
            MultipartFile archivo = archivosDocumentos.get(i);
            String descripcion = (descripcionesDocumentos != null && i < descripcionesDocumentos.size())
                    ? descripcionesDocumentos.get(i)
                    : "Documento " + (i + 1);
            try {
                log.debug("Re-post: subiendo documento {} '{}' para prepostulacion {}",
                        i + 1, descripcion, idNuevaPrepostulacion);
                String urlDoc = supabaseService.subirArchivo(
                        archivo, "documentos_academicos", tag + "_" + i);
                postgresProcedureRepository.agregarDocumentoPrepostulacion(
                        idNuevaPrepostulacion, descripcion, urlDoc);
                log.debug("Re-post: documento {} registrado correctamente", i + 1);
            } catch (Exception e) {
                log.error("Error al subir documento academico {} en re-postulacion: {}", i + 1, e.getMessage());
                throw new RuntimeException("Error al subir documento académico " + (i + 1) + ": " + e.getMessage());
            }
        }

        log.info("Re-postulacion {} completada con {} documento(s) academico(s)",
                idNuevaPrepostulacion, archivosDocumentos.size());

        // ── 5. Notificar al revisor ──────────────────────────────────────────
        try {
            Prepostulacion ultima = prepostulacionRepository
                    .findTopByIdentificacionOrderByFechaEnvioDesc(cedula)
                    .orElse(null);
            String nombreCompleto = ultima != null
                    ? ultima.getNombres() + " " + ultima.getApellidos()
                    : cedula;
            notifService.notifRevisorNuevaPrepostulacion(idNuevaPrepostulacion, nombreCompleto);
        } catch (Exception e) {
            log.warn("No se pudo enviar notificacion de re-postulacion: {}", e.getMessage());
        }

        // ── 6. Recuperar correo para la respuesta ────────────────────────────
        String correo = prepostulacionRepository
                .findTopByIdentificacionOrderByFechaEnvioDesc(cedula)
                .map(Prepostulacion::getCorreo)
                .orElse("");

        return new PrepostulacionResponseDTO(
                "Re-postulación enviada. Su solicitud está nuevamente en revisión.",
                correo, idNuevaPrepostulacion, true, LocalDateTime.now());
    }

    private String extraerMensajeTrigger(Exception e) {
        Throwable causa = e;
        while (causa.getCause() != null) causa = causa.getCause();
        String msg = causa.getMessage();
        if (msg != null && msg.contains("ERROR:")) {
            msg = msg.substring(msg.indexOf("ERROR:") + 6).trim();
            if (msg.contains("\n")) msg = msg.substring(0, msg.indexOf("\n")).trim();
            if (msg.contains("Where:")) msg = msg.substring(0, msg.indexOf("Where:")).trim();
        }
        return msg != null ? msg : "Error al procesar la solicitud.";
    }

    // =========================================================================
    // CONSULTAS
    // =========================================================================

    public Prepostulacion obtenerPorId(Long id) {
        return prepostulacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prepostulación no encontrada con ID: " + id));
    }

    public List<Prepostulacion> listarTodas() {
        return prepostulacionRepository.findAllByOrderByFechaEnvioDesc();
    }

    public List<Prepostulacion> listarPorEstado(String estado) {
        return prepostulacionRepository.findByEstadoRevision(estado);
    }

    public long contarPorEstado(String estado) {
        return prepostulacionRepository.findByEstadoRevision(estado).size();
    }

    public String obtenerEstadoPorCedula(String cedula) {
        Prepostulacion ultima = prepostulacionRepository
                .findTopByIdentificacionOrderByFechaEnvioDesc(cedula)
                .orElseThrow(() -> new RuntimeException("No existe ninguna solicitud con esta cédula."));
        return ultima.getEstadoRevision();
    }

    public List<Prepostulacion> buscar(String query) {
        List<Prepostulacion> todas = prepostulacionRepository.findAll();
        String queryLower = query.toLowerCase().trim();
        return todas.stream()
                .filter(p ->
                        p.getIdentificacion().toLowerCase().contains(queryLower) ||
                                p.getNombres().toLowerCase().contains(queryLower) ||
                                p.getApellidos().toLowerCase().contains(queryLower) ||
                                p.getCorreo().toLowerCase().contains(queryLower))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ACTUALIZAR ESTADO (aprobación / rechazo por admin)
    // =========================================================================

    @Transactional
    public void actualizarEstado(Long id, String nuevoEstado, String observaciones, Long idRevisor) {
        log.info("actualizarEstado -> id={}, estado={}", id, nuevoEstado);
        Prepostulacion prepostulacion = obtenerPorId(id);

        prepostulacion.setEstadoRevision(nuevoEstado);
        prepostulacion.setObservacionesRevision(observaciones);
        prepostulacion.setFechaRevision(LocalDateTime.now());
        prepostulacion.setIdRevisor(idRevisor);
        prepostulacionRepository.save(prepostulacion);

        log.info("Estado de prepostulacion {} actualizado a: {}", id, nuevoEstado);

        // ── APROBADO: Crear usuario + iniciar proceso de evaluación ──────────
        if ("APROBADO".equalsIgnoreCase(nuevoEstado)) {
            log.info("Creando usuario para postulante aprobado — prepostulacion {}", id);
            try {
                String correo     = prepostulacion.getCorreo();
                String base       = correo.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
                String usuarioApp = base;
                int n = 1;
                while (usuarioRepository.existsByUsuarioApp(usuarioApp)) {
                    usuarioApp = base + n++;
                }

                String usuarioBd      = generarUsuarioBdUnico(
                        normalizar(prepostulacion.getNombres()) + normalizar(prepostulacion.getApellidos()));
                String claveTemporal  = generarClaveTemporal(12);
                String claveBdReal    = generarClaveTemporal(16);
                String claveBdCifrada = aesCipherService.cifrar(claveBdReal);

                RegistroSpResultDTO resultado = postgresProcedureRepository.registrarPostulante(
                        usuarioApp,
                        passwordEncoder.encode(claveTemporal),
                        correo,
                        usuarioBd,
                        claveBdCifrada,
                        claveBdReal,
                        prepostulacion.getIdPrepostulacion()
                );

                log.info("Postulante registrado con ID usuario: {}", resultado.getIdUsuario());
                emailService.enviarCredenciales(correo, usuarioApp, claveTemporal);
                log.info("Correo de credenciales enviado a {}", correo);

                notifService.notifPostulanteAprobado(
                        resultado.getIdUsuario(),
                        prepostulacion.getIdPrepostulacion()
                );

                // ── Iniciar proceso de evaluación automáticamente ──────────────
                try {
                    Postulante postulante = postulanteRepository
                            .findByPrepostulacion_IdPrepostulacion(prepostulacion.getIdPrepostulacion())
                            .orElseThrow(() -> new RuntimeException("Postulante no encontrado"));

                    Long idPostulante = postulante.getIdPostulante();

                    List<Long> idsSolicitud = prepostulacionSolicitudRepository
                            .findByIdIdPrepostulacion(prepostulacion.getIdPrepostulacion())
                            .stream()
                            .map(ps -> ps.getId().getIdSolicitud())
                            .collect(Collectors.toList());

                    for (Long idSolicitud : idsSolicitud) {
                        try {
                            postgresProcedureRepository.iniciarProcesoEvaluacion(idPostulante, idSolicitud);
                            log.info("Proceso de evaluacion iniciado para postulante {} solicitud {}",
                                    idPostulante, idSolicitud);
                        } catch (Exception ex) {
                            log.warn("No se pudo iniciar proceso para solicitud {}: {}", idSolicitud, ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error al iniciar proceso de evaluacion: {}", e.getMessage(), e);
                }
                // ──────────────────────────────────────────────────────────────

            } catch (Exception e) {
                log.error("Error al crear usuario para prepostulacion {}: {}", id, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        // ── RECHAZADO: Enviar correo de rechazo ──────────────────────────────
        if ("RECHAZADO".equalsIgnoreCase(nuevoEstado)) {
            log.info("Enviando correo de rechazo para prepostulacion {}", id);
            try {
                emailService.enviarCorreoRechazo(
                        prepostulacion.getCorreo(),
                        prepostulacion.getNombres() + " " + prepostulacion.getApellidos(),
                        observaciones
                );
                log.info("Correo de rechazo enviado para prepostulacion {}", id);

                usuarioRepository.findByCorreo(prepostulacion.getCorreo()).ifPresent(u ->
                        notifService.notifPostulanteRechazado(
                                u.getIdUsuario(),
                                prepostulacion.getIdPrepostulacion(),
                                observaciones
                        )
                );
            } catch (Exception e) {
                log.error("Error al enviar correo de rechazo {}: {}", id, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        System.out.println("FIN DE actualizarEstado");
    }

    // =========================================================================
    // ELIMINAR
    // =========================================================================

    @Transactional
    public void eliminar(Long id) {
        Prepostulacion prepostulacion = obtenerPorId(id);
        try {
            if (prepostulacion.getUrlCedula() != null)
                supabaseService.eliminarArchivo(prepostulacion.getUrlCedula());
            if (prepostulacion.getUrlFoto() != null)
                supabaseService.eliminarArchivo(prepostulacion.getUrlFoto());
            if (prepostulacion.getUrlPrerrequisitos() != null)
                supabaseService.eliminarArchivo(prepostulacion.getUrlPrerrequisitos());
        } catch (Exception e) {
            log.warn("Error al eliminar archivos de Supabase: {}", e.getMessage());
        }
        prepostulacionRepository.deleteById(id);
        log.info("Prepostulacion {} eliminada correctamente", id);
    }

    // =========================================================================
    // HELPERS PRIVADOS
    // =========================================================================

    private String normalizar(String s) {
        if (s == null) return "";
        String t = s.toLowerCase().replaceAll("\\s+", "");
        t = t.replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ü","u")
                .replace("ñ","n");
        return t.replaceAll("[^a-z0-9]", "");
    }

    private String generarUsuarioBdUnico(String base) {
        if (base == null || base.isBlank()) throw new RuntimeException("No se pudo generar usuarioBd");
        String candidato = base;
        int n = 1;
        while (usuarioRepository.existsByUsuarioBd(candidato)) {
            n++;
            candidato = base + n;
        }
        return candidato;
    }

    private String generarClaveTemporal(int length) {
        final String ABC = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
        java.security.SecureRandom r = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ABC.charAt(r.nextInt(ABC.length())));
        return sb.toString();
    }
}