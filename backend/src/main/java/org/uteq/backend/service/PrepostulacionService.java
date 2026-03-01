package org.uteq.backend.service;

//import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.dto.RegistroSpResultDTO;
import org.uteq.backend.entity.*;
//import org.uteq.backend.entity.RolApp;
import org.uteq.backend.repository.*;
import org.uteq.backend.dto.PrepostulacionResponseDTO;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.service.AesCipherService;
import org.uteq.backend.dto.RegistroSpResultDTO;
//import org.springframework.transaction.support.TransactionSynchronization;
//import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.uteq.backend.repository.ConvocatoriaSolicitudRepository;
import java.lang.Long;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
//@RequiredArgsConstructor
public class PrepostulacionService {

    private final PrepostulacionRepository prepostulacionRepository;
    private final SupabaseStorageService supabaseService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
//    private final UsuarioCreadorService usuarioCreadorService;
//    private final DbRoleSyncService dbRoleSyncService;
//    private final IUsuarioRolRepository usuarioRolRepository;
//    private final IRolUsuarioRepository rolUsuarioRepository;
    private final RolAppRepository rolAppRepository;
    private final PostgresProcedureRepository postgresProcedureRepository;
    private final AesCipherService aesCipherService;
    private final PrepostulacionSolicitudRepository prepostulacionSolicitudRepository;
    private final ConvocatoriaSolicitudRepository convocatoriaSolicitudRepository;


    @PersistenceContext
    private EntityManager entityManager;

    public PrepostulacionService(
            PrepostulacionRepository prepostulacionRepository,
            SupabaseStorageService supabaseService,
            UsuarioRepository usuarioRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            // UsuarioCreadorService usuarioCreadorService,
            //  ELIMINAR ESTOS 3:
            // DbRoleSyncService dbRoleSyncService,
            // IUsuarioRolRepository usuarioRolRepository,
            // IRolUsuarioRepository rolUsuarioRepository
            //  AGREGAR ESTOS 2:
            RolAppRepository rolAppRepository,
            PostgresProcedureRepository postgresProcedureRepository,
            AesCipherService aesCipherService,
            PrepostulacionSolicitudRepository prepostulacionSolicitudRepository,
            ConvocatoriaSolicitudRepository convocatoriaSolicitudRepository
    ) {
        this.prepostulacionRepository = prepostulacionRepository;
        this.supabaseService = supabaseService;
        this.usuarioRepository = usuarioRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        //this.usuarioCreadorService = usuarioCreadorService;
        // ELIMINAR:
        // this.dbRoleSyncService = dbRoleSyncService;
        // this.usuarioRolRepository = usuarioRolRepository;
        // this.rolUsuarioRepository = rolUsuarioRepository;
        //  AGREGAR:
        this.rolAppRepository = rolAppRepository;
        this.postgresProcedureRepository = postgresProcedureRepository;
        this.aesCipherService = aesCipherService;
        this.prepostulacionSolicitudRepository = prepostulacionSolicitudRepository;
        this.convocatoriaSolicitudRepository = convocatoriaSolicitudRepository;
    }
    @Transactional
    public PrepostulacionResponseDTO procesarPrepostulacion(
            String correo,
            String cedula,
            String nombres,
            String apellidos,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            MultipartFile archivoPrerrequisitos,
            Long idConvocatoria
    ) {
        System.out.println("🔄 Procesando prepostulación para identificación: " + cedula);

        // Validar que no exista duplicado
        if (prepostulacionRepository.existsByIdentificacion(cedula)) {
            throw new RuntimeException("Ya existe una solicitud con esta identificación");
        }

        // Crear entidad
        Prepostulacion prepostulacion = new Prepostulacion();
        prepostulacion.setCorreo(correo);
        prepostulacion.setIdentificacion(cedula);
        prepostulacion.setNombres(nombres);
        prepostulacion.setApellidos(apellidos);
        prepostulacion.setEstadoRevision("PENDIENTE");
        prepostulacion.setFechaEnvio(LocalDateTime.now());

        // ✅ SUBIR ARCHIVOS A SUPABASE
        try {
            System.out.println("📤 Subiendo cédula a Supabase...");
            String urlCedula = supabaseService.subirArchivo(
                    archivoCedula,
                    "cedulas",
                    cedula
            );
            prepostulacion.setUrlCedula(urlCedula);

            System.out.println("📤 Subiendo foto a Supabase...");
            String urlFoto = supabaseService.subirArchivo(
                    archivoFoto,
                    "fotos",
                    cedula
            );
            prepostulacion.setUrlFoto(urlFoto);

            System.out.println("📤 Subiendo prerrequisitos a Supabase...");
            String urlPrerrequisitos = supabaseService.subirArchivo(
                    archivoPrerrequisitos,
                    "prerrequisitos",
                    cedula
            );
            prepostulacion.setUrlPrerrequisitos(urlPrerrequisitos);

            System.out.println("✅ Todos los archivos subidos exitosamente");

        } catch (Exception e) {
            System.err.println("❌ Error al subir archivos: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al subir archivos: " + e.getMessage());
        }

        // Guardar en BD
        Prepostulacion guardado = prepostulacionRepository.save(prepostulacion);
        System.out.println("💾 Prepostulación guardada en BD con ID: " + guardado.getIdPrepostulacion());

        // ─── Amarre prepostulacion ↔ solicitud ───────────────────────────────────
        if (idConvocatoria != null) {
            List<Long> solicitudes = obtenerSolicitudesDeConvocatoria(idConvocatoria);
            for (Long idSolicitud : solicitudes) {
                if (!prepostulacionSolicitudRepository
                        .existsByIdIdPrepostulacionAndIdIdSolicitud(
                                guardado.getIdPrepostulacion(), idSolicitud)) {
                    prepostulacionSolicitudRepository.save(
                            new PrepostulacionSolicitud(guardado.getIdPrepostulacion(), idSolicitud)
                    );
                }
            }
            System.out.println("✅ Prepostulacion " + guardado.getIdPrepostulacion()
                    + " amarrada a convocatoria " + idConvocatoria);
        }

        return new PrepostulacionResponseDTO(
                "Solicitud registrada exitosamente",
                guardado.getCorreo(),
                guardado.getIdPrepostulacion(),
                true,
                guardado.getFechaEnvio()
        );
    }

    /**
     * Obtener una prepostulación por ID
     */
    public Prepostulacion obtenerPorId(Long id) {
        return prepostulacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prepostulación no encontrada con ID: " + id));
    }

    /**
     * Listar todas las prepostulaciones (más recientes primero)
     */
    public List<Prepostulacion> listarTodas() {
        return prepostulacionRepository.findAllByOrderByFechaEnvioDesc();
    }

    /**
     * Listar por estado de revisión
     */
    public List<Prepostulacion> listarPorEstado(String estado) {
        return prepostulacionRepository.findByEstadoRevision(estado);
    }

    // ============================================================
// SOLUCIÓN DEFINITIVA - TODO INLINE EN actualizarEstado
// ============================================================
// Ve a PrepostulacionService.java
// Busca el método actualizarEstado (Ctrl+F)
// BORRA TODO el método desde @Transactional hasta su cierre }
// PEGA ESTO:

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

        // ═══════════════════════════════════════════════════════════
        // APROBADO: Crear usuario y enviar credenciales
        // ═══════════════════════════════════════════════════════════
        if ("APROBADO".equalsIgnoreCase(nuevoEstado)) {
            System.out.println("\n🎯 CREANDO USUARIO PARA POSTULANTE APROBADO");

            try {
                String correo = prepostulacion.getCorreo();
                System.out.println("Correo: " + correo);

                // 1. Generar usuarioApp
                String base = correo.split("@")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
                String usuarioApp = base;
                int n = 1;
                while (usuarioRepository.existsByUsuarioApp(usuarioApp)) {
                    usuarioApp = base + n;
                    n++;
                }
                System.out.println("✅ usuarioApp: " + usuarioApp);

                // 2. Generar usuarioBd
                String nombres = prepostulacion.getNombres().toLowerCase()
                        .replace("á","a").replace("é","e").replace("í","i")
                        .replace("ó","o").replace("ú","u").replace("ñ","n")
                        .replaceAll("[^a-z0-9]", "");
                String apellidos = prepostulacion.getApellidos().toLowerCase()
                        .replace("á","a").replace("é","e").replace("í","i")
                        .replace("ó","o").replace("ú","u").replace("ñ","n")
                        .replaceAll("[^a-z0-9]", "");
                String usuarioBd = nombres + apellidos;
                int m = 1;
                while (usuarioRepository.existsByUsuarioBd(usuarioBd)) {
                    usuarioBd = nombres + apellidos + m;
                    m++;
                }
                System.out.println("✅ usuarioBd: " + usuarioBd);

                // 3. Generar clave temporal
                String claveTemporal = generarClaveTemporal(12);
                System.out.println("✅ Clave temporal generada");

                // ✅ 4. SP hace todo: INSERT usuario + CREATE USER + GRANT roles
                String claveBdReal    = generarClaveTemporal(16);
                String claveBdCifrada = aesCipherService.cifrar(claveBdReal);

                System.out.println("💾 Registrando postulante con SP...");
                RegistroSpResultDTO resultado = postgresProcedureRepository.registrarPostulante(
                        usuarioApp,
                        passwordEncoder.encode(claveTemporal),
                        correo,
                        usuarioBd,
                        claveBdCifrada,
                        claveBdReal
                );
                System.out.println("✅✅✅ POSTULANTE REGISTRADO CON ID: " + resultado.getIdUsuario());

                // 5. Enviar correo con credenciales
                System.out.println("📧 Enviando correo con credenciales...");
                emailService.enviarCredenciales(correo, usuarioApp, claveTemporal);
                System.out.println("✅ Correo enviado exitosamente");

            } catch (Exception e) {
                System.err.println("❌ ERROR al crear usuario: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // ═══════════════════════════════════════════════════════════
        // RECHAZADO: Enviar correo de rechazo
        // ═══════════════════════════════════════════════════════════
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

    /**
     * Buscar prepostulaciones por identificación, nombre o apellido
     */
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

    /**
     * Eliminar una prepostulación
     * IMPORTANTE: También elimina los archivos de Supabase
     */
    @Transactional
    public void eliminar(Long id) {
        Prepostulacion prepostulacion = obtenerPorId(id);

        // Eliminar archivos de Supabase primero
        try {
            if (prepostulacion.getUrlCedula() != null) {
                supabaseService.eliminarArchivo(prepostulacion.getUrlCedula());
            }
            if (prepostulacion.getUrlFoto() != null) {
                supabaseService.eliminarArchivo(prepostulacion.getUrlFoto());
            }
            if (prepostulacion.getUrlPrerrequisitos() != null) {
                supabaseService.eliminarArchivo(prepostulacion.getUrlPrerrequisitos());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al eliminar archivos de Supabase: " + e.getMessage());
            // Continuamos con la eliminación de la BD aunque falle Supabase
        }

        // Eliminar de la base de datos
        prepostulacionRepository.deleteById(id);

        System.out.println("🗑️ Prepostulación " + id + " eliminada correctamente");
    }

    /**
     * Contar prepostulaciones por estado
     */
    public long contarPorEstado(String estado) {
        return prepostulacionRepository.findByEstadoRevision(estado).size();
    }

    // ===============================
    // GENERACIÓN DE CREDENCIALES
    // ===============================

    /**
     * Genera un usuario app a partir del correo + 4 dígitos aleatorios
     * Ejemplo: test@ejemplo.com -> test1234
     */
    private String generarUsuarioApp(String correo) {
        String base = correo.split("@")[0]; // Toma lo que está antes del @
        int aleatorio = (int) (Math.random() * 9000) + 1000; // Número entre 1000 y 9999
        return base + aleatorio;
    }

    /**
     * Genera una contraseña temporal aleatoria de 12 caracteres
     */
    private String generarClaveTemporal() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder clave = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            int index = (int) (Math.random() * caracteres.length());
            clave.append(caracteres.charAt(index));
        }
        return clave.toString();
    }

    private void crearUsuarioParaPrepostulacion(Prepostulacion prepostulacion) {
        try {
            System.out.println("\n📝 Iniciando creación de usuario...");
            System.out.println("📝 Correo del postulante: " + prepostulacion.getCorreo());

            // Generar usuarioApp (igual que en AutoridadAcademicaServiceImpl)
            String usuarioApp = generarUsuarioAppDesdeCorreo(prepostulacion.getCorreo());
            System.out.println("✅ Usuario App generado: " + usuarioApp);

            // Generar usuarioBd (igual que en AutoridadAcademicaServiceImpl)
            String baseBd = generarUsuarioBdBase(prepostulacion.getNombres(), prepostulacion.getApellidos());
            String usuarioBd = generarUsuarioBdUnico(baseBd);
            System.out.println("✅ Usuario BD generado: " + usuarioBd);

            // Generar clave temporal (igual que en AutoridadAcademicaServiceImpl)
            String claveTemporal = generarClaveTemporal(12);
            System.out.println("✅ Clave temporal generada (12 caracteres)");

            // Hashear la clave
            String claveHash = passwordEncoder.encode(claveTemporal);
            System.out.println("✅ Clave hasheada correctamente");

            // Crear usuario (igual que en AutoridadAcademicaServiceImpl)
            Usuario usuario = new Usuario();
            usuario.setUsuarioApp(usuarioApp);
            usuario.setClaveApp(claveHash);
            usuario.setCorreo(prepostulacion.getCorreo());
            usuario.setUsuarioBd(usuarioBd);
            usuario.setClaveBd("MTIzNA=="); // Igual que en AutoridadAcademicaServiceImpl
            usuario.setActivo(true);

            System.out.println("💾 Guardando usuario en base de datos...");
            Usuario usuarioGuardado = usuarioRepository.save(usuario);
            System.out.println("✅ Usuario guardado exitosamente con ID: " + usuarioGuardado.getIdUsuario());

            // Enviar correo con credenciales (igual que en AutoridadAcademicaServiceImpl)
            System.out.println("📧 Enviando correo con credenciales...");
            emailService.enviarCredenciales(
                    prepostulacion.getCorreo(),
                    usuarioApp,
                    claveTemporal
            );
            System.out.println("✅ Correo de credenciales enviado exitosamente");

        } catch (Exception e) {
            System.err.println("\n❌❌❌ ERROR AL CREAR USUARIO ❌❌❌");
            System.err.println("❌ Mensaje: " + e.getMessage());
            System.err.println("❌ Tipo: " + e.getClass().getName());
            System.err.println("❌ Stack trace completo:");
            e.printStackTrace();
            // No lanzamos excepción para que no falle toda la aprobación
        }
    }

    private void enviarCorreoRechazo(Prepostulacion prepostulacion, String motivo) {
        try {
            System.out.println("\n📧 Enviando correo de rechazo...");
            System.out.println("📧 Destinatario: " + prepostulacion.getCorreo());
            System.out.println("📧 Motivo: " + motivo);

            emailService.enviarCorreoRechazo(
                    prepostulacion.getCorreo(),
                    prepostulacion.getNombres() + " " + prepostulacion.getApellidos(),
                    motivo
            );

            System.out.println("✅ Correo de rechazo enviado exitosamente");

        } catch (Exception e) {
            System.err.println("\n❌❌❌ ERROR AL ENVIAR CORREO DE RECHAZO ❌❌❌");
            System.err.println("❌ Mensaje: " + e.getMessage());
            System.err.println("❌ Tipo: " + e.getClass().getName());
            e.printStackTrace();
        }
    }

// 4️⃣ AGREGA estos métodos helper (copiados EXACTAMENTE de AutoridadAcademicaServiceImpl)
// Si ya existen, reemplázalos

    private String generarUsuarioAppDesdeCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            throw new RuntimeException("Correo inválido para generar usuarioApp");
        }
        String base = correo.split("@")[0].trim().toLowerCase();
        base = base.replaceAll("\\s+", "").replaceAll("[^a-z0-9._-]", "");
        if (base.isBlank()) throw new RuntimeException("No se pudo generar usuarioApp");

        String candidato = base;
        int n = 1;
        while (usuarioRepository.existsByUsuarioApp(candidato)) {
            n++;
            candidato = base + n;
        }
        return candidato;
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String t = s.toLowerCase();
        t = t.replaceAll("\\s+", "");
        t = t.replace("á","a").replace("é","e").replace("í","i")
                .replace("ó","o").replace("ú","u").replace("ü","u")
                .replace("ñ","n");
        return t.replaceAll("[^a-z0-9]", "");
    }

    private String generarUsuarioBdBase(String nombres, String apellidos) {
        return normalizar(nombres) + normalizar(apellidos);
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
        for (int i = 0; i < length; i++) {
            sb.append(ABC.charAt(r.nextInt(ABC.length())));
        }
        return sb.toString();
    }


    @Transactional
    public PrepostulacionResponseDTO repostular(
            String cedula,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            MultipartFile archivoPrerrequisitos,
            Long idSolicitud
    ) {
        // 1. Buscar la última postulación de esta cédula
        Prepostulacion ultima = prepostulacionRepository
                .findTopByIdentificacionOrderByFechaEnvioDesc(cedula)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró ninguna solicitud con esta cédula. " +
                                "Si es la primera vez, use el formulario de registro."));

        // 2. Solo puede re-postular si fue rechazada
        if (!"RECHAZADO".equalsIgnoreCase(ultima.getEstadoRevision())) {
            throw new RuntimeException(
                    "Su solicitud tiene estado '" + ultima.getEstadoRevision() + "'. " +
                            "Solo puede re-postular si fue rechazada.");
        }

        // 3. Subir nuevos documentos a Supabase
        String urlCedula, urlFoto, urlPrerrequisitos;
        try {
            String tag = cedula + "_repost_" + System.currentTimeMillis();
            urlCedula         = supabaseService.subirArchivo(archivoCedula,          "cedulas",         tag);
            urlFoto           = supabaseService.subirArchivo(archivoFoto,            "fotos",           tag);
            urlPrerrequisitos = supabaseService.subirArchivo(archivoPrerrequisitos,  "prerrequisitos",  tag);
        } catch (Exception e) {
            throw new RuntimeException("Error al subir documentos: " + e.getMessage());
        }

        // 4. Guardar NUEVA fila via stored procedure (historial completo)
        Long idNuevaPrepostulacion = postgresProcedureRepository.registrarPrepostulacion(
                ultima.getNombres(),
                ultima.getApellidos(),
                cedula,
                ultima.getCorreo(),
                ultima.getTelefono(),
                ultima.getFechaNacimiento() != null ? ultima.getFechaNacimiento().toLocalDate() : null,
                urlCedula,
                urlFoto,
                urlPrerrequisitos,
                idSolicitud
        );

        System.out.println("💾 Re-postulación guardada como nueva fila con ID: " + idNuevaPrepostulacion);

        return new PrepostulacionResponseDTO(
                "Re-postulación enviada. Su solicitud está nuevamente en revisión.",
                ultima.getCorreo(),
                idNuevaPrepostulacion,
                true,
                LocalDateTime.now()
        );
    }

    public String obtenerEstadoPorCedula(String cedula) {
        Prepostulacion ultima = prepostulacionRepository
                .findTopByIdentificacionOrderByFechaEnvioDesc(cedula)
                .orElseThrow(() -> new RuntimeException("No existe ninguna solicitud con esta cédula."));
        return ultima.getEstadoRevision();
    }

    private List<Long> obtenerSolicitudesDeConvocatoria(Long idConvocatoria) {
        return convocatoriaSolicitudRepository
                .findByIdConvocatoria(idConvocatoria)
                .stream()
                .map(ConvocatoriaSolicitud::getIdSolicitud)
                .collect(Collectors.toList());
    }
}
