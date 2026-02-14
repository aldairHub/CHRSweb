package org.uteq.backend.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.Entity.*;
import org.uteq.backend.Repository.IRolUsuarioRepository;
import org.uteq.backend.Repository.IUsuarioRolRepository;
import org.uteq.backend.Repository.PrepostulacionRepository;
import org.uteq.backend.Repository.UsuarioRepository;
import org.uteq.backend.dto.PrepostulacionResponseDTO;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrepostulacionService {

    private final PrepostulacionRepository prepostulacionRepository;
    private final SupabaseStorageService supabaseService;
    private final UsuarioRepository usuarioRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioCreadorService usuarioCreadorService;
    private final DbRoleSyncService dbRoleSyncService;
    private final IUsuarioRolRepository usuarioRolRepository;
    private final IRolUsuarioRepository rolUsuarioRepository;

    @Transactional
    public PrepostulacionResponseDTO procesarPrepostulacion(
            String correo,
            String cedula,
            String nombres,
            String apellidos,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            MultipartFile archivoPrerrequisitos
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
                String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@#$%";
                StringBuilder clave = new StringBuilder();
                java.security.SecureRandom random = new java.security.SecureRandom();
                for (int i = 0; i < 12; i++) {
                    clave.append(caracteres.charAt(random.nextInt(caracteres.length())));
                }
                String claveTemporal = clave.toString();
                System.out.println("✅ Clave temporal generada");

                // 4. Crear usuario
                Usuario usuario = new Usuario();
                usuario.setUsuarioApp(usuarioApp);
                usuario.setClaveApp(passwordEncoder.encode(claveTemporal));
                usuario.setCorreo(correo);
                usuario.setUsuarioBd(usuarioBd);
                usuario.setClaveBd("MTIzNA==");
                usuario.setActivo(true);

                System.out.println("💾 Guardando usuario en BD...");
                Usuario usuarioGuardado = usuarioRepository.save(usuario);
                RolUsuario rolPostulante = rolUsuarioRepository.findByNombre("ROLE_POSTULANTE")
                        .orElseThrow(() -> new RuntimeException("Rol ROLE_POSTULANTE no existe"));

                UsuarioRolId usuarioRolId = new UsuarioRolId();
                usuarioRolId.setIdUsuario(usuarioGuardado.getIdUsuario());
                usuarioRolId.setIdRolUsuario(rolPostulante.getIdRolUsuario());

                UsuarioRol usuarioRol = new UsuarioRol();
                usuarioRol.setId(usuarioRolId);  // ← OJO: usuarioRolId no id
                usuarioRol.setUsuario(usuarioGuardado);  // ← OJO: usuarioGuardado
                usuarioRol.setRol(rolPostulante);

                usuarioRolRepository.save(usuarioRol);
                System.out.println("✅ Rol ROLE_POSTULANTE asignado");
                System.out.println("✅✅✅ USUARIO GUARDADO CON ID: " + usuarioGuardado.getIdUsuario());
                //Long idUsuario = usuarioGuardado.getIdUsuario();
                TransactionSynchronizationManager.registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                try {
                                    System.out.println("🔄 Sincronizando roles en BD después del commit...");
                                    dbRoleSyncService.syncRolesUsuarioBd(usuarioGuardado.getIdUsuario().intValue(), false);
                                    System.out.println("✅ Roles sincronizados en BD");
                                } catch (Exception ex) {
                                    System.err.println("⚠️ Error al sincronizar roles en BD: " + ex.getMessage());
                                    ex.printStackTrace();
                                    // No lanzamos excepción aquí porque ya se hizo commit
                                }
                            }
                        }
                );

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
}