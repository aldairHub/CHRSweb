package org.uteq.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.dto.RegistroSpResultDTO;
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
    private static final Logger log =
            LoggerFactory.getLogger(PrepostulacionService.class);

    private final PrepostulacionRepository prepostulacionRepository;
    private final SupabaseStorageService supabaseService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RolAppRepository rolAppRepository;
    private final PostgresProcedureRepository postgresProcedureRepository;
    private final AesCipherService aesCipherService;
    private final PrepostulacionDocumentoRepository documentoRepository;
    public PrepostulacionService(
            PrepostulacionRepository prepostulacionRepository,
            SupabaseStorageService supabaseService,
            UsuarioRepository usuarioRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            RolAppRepository rolAppRepository,
            PostgresProcedureRepository postgresProcedureRepository,
            AesCipherService aesCipherService,
            PrepostulacionDocumentoRepository documentoRepository
    ) {
        this.prepostulacionRepository = prepostulacionRepository;
        this.supabaseService = supabaseService;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rolAppRepository = rolAppRepository;
        this.postgresProcedureRepository = postgresProcedureRepository;
        this.aesCipherService = aesCipherService;
        this.documentoRepository = documentoRepository;
    }

    // =========================================================================
    // REGISTRO INICIAL
    // =========================================================================

    /**
     * Registra una nueva prepostulación y la asocia a la solicitud elegida por el postulante.
     *
     **/
    @Transactional
    public PrepostulacionResponseDTO procesarPrepostulacion(
            String correo,
            String cedula,
            String nombres,
            String apellidos,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            List<MultipartFile> archivosDocumentos,      // NUEVO: lista de PDFs
            List<String> descripcionesDocumentos,         // NUEVO: lista de descripciones
            Long idSolicitud
    ) {
        log.info("Procesando prepostulacion para identificacion: {}", cedula);

        // Subir cédula y foto (sin cambios)
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

        // Guardar prepostulacion en BD via stored procedure (sin url_prerrequisitos)
        Long idPrepostulacion = postgresProcedureRepository.registrarPrepostulacion(
                nombres,
                apellidos,
                cedula,
                correo,
                urlCedula,
                urlFoto,
                idSolicitud
        );

        log.info("Prepostulacion guardada con ID: {}", idPrepostulacion);
        if (idSolicitud != null) {
            log.info("Prepostulacion {} vinculada a solicitud {}", idPrepostulacion, idSolicitud);
        }

        // Subir y registrar cada documento académico individualmente
        String tag = cedula + "_" + System.currentTimeMillis();
        for (int i = 0; i < archivosDocumentos.size(); i++) {
            MultipartFile archivo = archivosDocumentos.get(i);
            String descripcion = (descripcionesDocumentos != null && i < descripcionesDocumentos.size())
                    ? descripcionesDocumentos.get(i)
                    : "Documento " + (i + 1);
            try {
                log.debug("Subiendo documento academico {} de {}: {}", i + 1, archivosDocumentos.size(), descripcion);
                String urlDoc = supabaseService.subirArchivo(archivo, "documentos_academicos", tag + "_" + i);

                postgresProcedureRepository.agregarDocumentoPrepostulacion(
                        idPrepostulacion,
                        descripcion,
                        urlDoc
                );
                log.debug("Documento {} registrado correctamente", i + 1);
            } catch (Exception e) {
                log.error("Error al subir documento academico {}: {}", i + 1, e.getMessage());
                throw new RuntimeException("Error al subir documento académico " + (i + 1) + ": " + e.getMessage());
            }
        }

        log.info("Prepostulacion {} completada con {} documento(s) academico(s)",
                idPrepostulacion, archivosDocumentos.size());

        return new PrepostulacionResponseDTO(
                "Solicitud registrada exitosamente",
                correo,
                idPrepostulacion,
                true,
                LocalDateTime.now()
        );
    }
//    @Transactional
//    public PrepostulacionResponseDTO procesarPrepostulacion(
//            String correo,
//            String cedula,
//            String nombres,
//            String apellidos,
//            MultipartFile archivoCedula,
//            MultipartFile archivoFoto,
//            MultipartFile archivoPrerrequisitos,
//            Long idSolicitud
//    ) {
////        System.out.println("🔄 Procesando prepostulación para identificación: " + cedula);
//        log.info("Procesando prepostulacion para identificacion: {}", cedula);
//        // Subir archivos a Supabase
//        String urlCedula, urlFoto, urlPrerrequisitos;
//        try {
////            System.out.println("📤 Subiendo cédula a Supabase...");
//            log.debug("Subiendo id a Supabase...");
//            urlCedula = supabaseService.subirArchivo(archivoCedula, "cedulas", cedula);
//
////            System.out.println("📤 Subiendo foto a Supabase...");
//            log.debug("Subiendo foto a Supabase...");
//            urlFoto = supabaseService.subirArchivo(archivoFoto, "fotos", cedula);
//
////            System.out.println("📤 Subiendo prerrequisitos a Supabase...");
//            log.debug("Subiendo prerrequisitos a Supabase...");
//            urlPrerrequisitos = supabaseService.subirArchivo(archivoPrerrequisitos, "prerrequisitos", cedula);
//
////            System.out.println("✅ Todos los archivos subidos exitosamente");
//            log.info("Archivos de prepostulacion subidos exitosamente");
//        } catch (Exception e) {
////            System.err.println("❌ Error al subir archivos: " + e.getMessage());
//            log.error("Error al subir archivos: {}", e.getMessage());
//            throw new RuntimeException("Error al subir archivos: " + e.getMessage());
//        }
//
//        // Guardar en BD via stored procedure
//        Long idPrepostulacion = postgresProcedureRepository.registrarPrepostulacion(
//                nombres,
//                apellidos,
//                cedula,
//                correo,
//                urlCedula,
//                urlFoto,
//                urlPrerrequisitos,
//                idSolicitud
//        );
//
////        System.out.println("💾 Prepostulación guardada en BD con ID: " + idPrepostulacion);
//         log.info("Prepostulacion guardada con ID: {}", idPrepostulacion);
//        if (idSolicitud != null) {
////            System.out.println("✅ Prepostulacion " + idPrepostulacion + " amarrada a solicitud " + idSolicitud);
//             log.info("Prepostulacion {} vinculada a solicitud {}",
//                    idPrepostulacion, idSolicitud);
//
//        }
//
//        return new PrepostulacionResponseDTO(
//                "Solicitud registrada exitosamente",
//                correo,
//                idPrepostulacion,
//                true,
//                LocalDateTime.now()
//        );
//    }

    // =========================================================================
    // REPOSTULACIÓN
    // =========================================================================

    /**
     * Re-postulación: crea una NUEVA fila en prepostulacion (mantiene historial).
     * Solo permitido si la última postulación de esa cédula fue RECHAZADA.
     * La validación del estado RECHAZADO es delegada al stored procedure (atomicidad garantizada).
     *
     * @param idSolicitud La solicitud a la que aplica ahora (puede ser distinta a la anterior)
     */
    @Transactional
    public PrepostulacionResponseDTO repostular(
            String cedula,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            MultipartFile archivoPrerrequisitos,
            Long idSolicitud
    ) {
        // 1. Subir nuevos documentos a Supabase
        String urlCedula, urlFoto, urlPrerrequisitos;
        try {
            String tag = cedula + "_repost_" + System.currentTimeMillis();
            urlCedula         = supabaseService.subirArchivo(archivoCedula,         "cedulas",        tag);
            urlFoto           = supabaseService.subirArchivo(archivoFoto,           "fotos",          tag);
            urlPrerrequisitos = supabaseService.subirArchivo(archivoPrerrequisitos, "prerrequisitos", tag);
        } catch (Exception e) {
            throw new RuntimeException("Error al subir documentos: " + e.getMessage());
        }

        // 2. El SP valida RECHAZADO, copia datos anteriores e inserta nueva fila
        Long idNuevaPrepostulacion = postgresProcedureRepository.repostular(
                cedula, idSolicitud, urlCedula, urlFoto, urlPrerrequisitos
        );

        System.out.println("💾 Re-postulación guardada como nueva fila con ID: " + idNuevaPrepostulacion);

        // 3. Obtener correo del registro más reciente para la respuesta
        String correo = prepostulacionRepository
                .findTopByIdentificacionOrderByFechaEnvioDesc(cedula)
                .map(Prepostulacion::getCorreo)
                .orElse("");

        return new PrepostulacionResponseDTO(
                "Re-postulación enviada. Su solicitud está nuevamente en revisión.",
                correo,
                idNuevaPrepostulacion,
                true,
                LocalDateTime.now()
        );
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

    /**
     * Retorna el estado de la ÚLTIMA postulación de una cédula.
     */
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
                                p.getCorreo().toLowerCase().contains(queryLower)
                )
                .collect(Collectors.toList());
    }

    // =========================================================================
    // ACTUALIZAR ESTADO (aprobación / rechazo por admin)
    // =========================================================================

    @Transactional
    public void actualizarEstado(Long id, String nuevoEstado, String observaciones, Long idRevisor) {

//        System.out.println("\n═══════════════════════════════════════════");
//        System.out.println("METODO actualizarEstado LLAMADO");
//        System.out.println("ID: " + id + " | Estado: " + nuevoEstado);
//        System.out.println("═══════════════════════════════════════════");
        log.info("actualizarEstado -> id={}, estado={}", id, nuevoEstado);
        Prepostulacion prepostulacion = obtenerPorId(id);

        prepostulacion.setEstadoRevision(nuevoEstado);
        prepostulacion.setObservacionesRevision(observaciones);
        prepostulacion.setFechaRevision(LocalDateTime.now());
        prepostulacion.setIdRevisor(idRevisor);

        prepostulacionRepository.save(prepostulacion);

//        System.out.println("✅ Estado de prepostulación " + id + " actualizado a: " + nuevoEstado);
        log.info("Estado de prepostulacion {} actualizado a: {}", id, nuevoEstado);

        // APROBADO: Crear usuario y enviar credenciales
        if ("APROBADO".equalsIgnoreCase(nuevoEstado)) {
//            System.out.println("\n🎯 CREANDO USUARIO PARA POSTULANTE APROBADO");
            log.info("Creando usuario para postulante aprobado — prepostulacion {}", id);
            try {
                String correo    = prepostulacion.getCorreo();
                String base      = correo.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
                String usuarioApp = base;
                int n = 1;
                while (usuarioRepository.existsByUsuarioApp(usuarioApp)) {
                    usuarioApp = base + n++;
                }

                String usuarioBd = generarUsuarioBdUnico(
                        normalizar(prepostulacion.getNombres()) + normalizar(prepostulacion.getApellidos())
                );

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
                        prepostulacion.getIdPrepostulacion()   // ✅ CLAVE

                );
//                System.out.println("✅✅✅ POSTULANTE REGISTRADO CON ID: " + resultado.getIdUsuario());
                log.info("Postulante registrado con ID: {}", resultado.getIdUsuario());
                emailService.enviarCredenciales(correo, usuarioApp, claveTemporal);
//                System.out.println("✅ Correo enviado exitosamente");
                 log.info("Correo de credenciales enviado a {}", correo);
            } catch (Exception e) {
//                System.err.println("❌ ERROR al crear usuario: " + e.getMessage());
                log.error("Error al crear usuario para prepostulacion {}: {}", id, e.getMessage(), e);
                e.printStackTrace();
            }
        }

        // RECHAZADO: Enviar correo de rechazo
        if ("RECHAZADO".equalsIgnoreCase(nuevoEstado)) {
//            System.out.println("\n❌ ENVIANDO CORREO DE RECHAZO");
            log.info("Enviando correo de rechazo para prepostulacion {}", id);
            try {
                emailService.enviarCorreoRechazo(
                        prepostulacion.getCorreo(),
                        prepostulacion.getNombres() + " " + prepostulacion.getApellidos(),
                        observaciones
                );
//                System.out.println("✅ Correo de rechazo enviado");
                log.info("Correo de rechazo enviado para prepostulacion {}", id);
            } catch (Exception e) {
//                System.err.println("❌ ERROR al enviar correo de rechazo: " + e.getMessage());
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
//            System.err.println("⚠️ Error al eliminar archivos de Supabase: " + e.getMessage());
            log.warn("Error al eliminar archivos de Supabase: {}", e.getMessage());
        }
        prepostulacionRepository.deleteById(id);
//        System.out.println("🗑️ Prepostulación " + id + " eliminada correctamente");
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