package org.uteq.backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.dto.RegistroSpResultDTO;
import org.uteq.backend.entity.Prepostulacion;
import org.uteq.backend.repository.*;
import org.uteq.backend.dto.PrepostulacionResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrepostulacionService {

    private final PrepostulacionRepository prepostulacionRepository;
    private final SupabaseStorageService supabaseService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RolAppRepository rolAppRepository;
    private final PostgresProcedureRepository postgresProcedureRepository;
    private final AesCipherService aesCipherService;

    public PrepostulacionService(
            PrepostulacionRepository prepostulacionRepository,
            SupabaseStorageService supabaseService,
            UsuarioRepository usuarioRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            RolAppRepository rolAppRepository,
            PostgresProcedureRepository postgresProcedureRepository,
            AesCipherService aesCipherService
    ) {
        this.prepostulacionRepository = prepostulacionRepository;
        this.supabaseService = supabaseService;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rolAppRepository = rolAppRepository;
        this.postgresProcedureRepository = postgresProcedureRepository;
        this.aesCipherService = aesCipherService;
    }

    // =========================================================================
    // REGISTRO INICIAL
    // =========================================================================

    /**
     * Registra una nueva prepostulación y la asocia a la solicitud elegida por el postulante.
     * Todo se hace via stored procedure (atomicidad garantizada).
     *
     * @param idSolicitud ID de la solicitud_docente específica que el postulante seleccionó
     */
    @Transactional
    public PrepostulacionResponseDTO procesarPrepostulacion(
            String correo,
            String cedula,
            String nombres,
            String apellidos,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            MultipartFile archivoPrerrequisitos,
            Long idSolicitud
    ) {
        System.out.println("🔄 Procesando prepostulación para identificación: " + cedula);

        // Subir archivos a Supabase
        String urlCedula, urlFoto, urlPrerrequisitos;
        try {
            System.out.println("📤 Subiendo cédula a Supabase...");
            urlCedula = supabaseService.subirArchivo(archivoCedula, "cedulas", cedula);

            System.out.println("📤 Subiendo foto a Supabase...");
            urlFoto = supabaseService.subirArchivo(archivoFoto, "fotos", cedula);

            System.out.println("📤 Subiendo prerrequisitos a Supabase...");
            urlPrerrequisitos = supabaseService.subirArchivo(archivoPrerrequisitos, "prerrequisitos", cedula);

            System.out.println("✅ Todos los archivos subidos exitosamente");
        } catch (Exception e) {
            System.err.println("❌ Error al subir archivos: " + e.getMessage());
            throw new RuntimeException("Error al subir archivos: " + e.getMessage());
        }

        // Guardar en BD via stored procedure
        Long idPrepostulacion = postgresProcedureRepository.registrarPrepostulacion(
                nombres,
                apellidos,
                cedula,
                correo,
                urlCedula,
                urlFoto,
                urlPrerrequisitos,
                idSolicitud
        );

        System.out.println("💾 Prepostulación guardada en BD con ID: " + idPrepostulacion);
        if (idSolicitud != null) {
            System.out.println("✅ Prepostulacion " + idPrepostulacion + " amarrada a solicitud " + idSolicitud);
        }

        return new PrepostulacionResponseDTO(
                "Solicitud registrada exitosamente",
                correo,
                idPrepostulacion,
                true,
                LocalDateTime.now()
        );
    }

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

        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("MÉTODO actualizarEstado LLAMADO");
        System.out.println("ID: " + id + " | Estado: " + nuevoEstado);
        System.out.println("═══════════════════════════════════════════");

        Prepostulacion prepostulacion = obtenerPorId(id);

        prepostulacion.setEstadoRevision(nuevoEstado);
        prepostulacion.setObservacionesRevision(observaciones);
        prepostulacion.setFechaRevision(LocalDateTime.now());
        prepostulacion.setIdRevisor(idRevisor);

        prepostulacionRepository.save(prepostulacion);

        System.out.println("✅ Estado de prepostulación " + id + " actualizado a: " + nuevoEstado);

        // APROBADO: Crear usuario y enviar credenciales
        if ("APROBADO".equalsIgnoreCase(nuevoEstado)) {
            System.out.println("\n🎯 CREANDO USUARIO PARA POSTULANTE APROBADO");
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
                System.out.println("✅✅✅ POSTULANTE REGISTRADO CON ID: " + resultado.getIdUsuario());

                emailService.enviarCredenciales(correo, usuarioApp, claveTemporal);
                System.out.println("✅ Correo enviado exitosamente");

            } catch (Exception e) {
                System.err.println("❌ ERROR al crear usuario: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // RECHAZADO: Enviar correo de rechazo
        if ("RECHAZADO".equalsIgnoreCase(nuevoEstado)) {
            System.out.println("\n❌ ENVIANDO CORREO DE RECHAZO");
            try {
                emailService.enviarCorreoRechazo(
                        prepostulacion.getCorreo(),
                        prepostulacion.getNombres() + " " + prepostulacion.getApellidos(),
                        observaciones
                );
                System.out.println("✅ Correo de rechazo enviado");
            } catch (Exception e) {
                System.err.println("❌ ERROR al enviar correo de rechazo: " + e.getMessage());
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
            System.err.println("⚠️ Error al eliminar archivos de Supabase: " + e.getMessage());
        }
        prepostulacionRepository.deleteById(id);
        System.out.println("🗑️ Prepostulación " + id + " eliminada correctamente");
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