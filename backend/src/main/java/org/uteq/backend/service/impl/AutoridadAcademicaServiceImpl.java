package org.uteq.backend.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.dto.*;
import org.uteq.backend.entity.AutoridadAcademica;
import org.uteq.backend.entity.Usuario;

import org.uteq.backend.repository.AutoridadAcademicaRepository;
//import org.uteq.backend.repository.IRolAutoridadRepository;
import org.uteq.backend.repository.InstitucionRepository;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.repository.UsuarioRepository;

import org.uteq.backend.service.AesCipherService;
import org.uteq.backend.service.AutoridadAcademicaService;
import org.uteq.backend.service.DbRoleSyncService;
import org.uteq.backend.service.EmailService;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import org.uteq.backend.dto.RolAppDTO;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uteq.backend.util.CredencialesGenerator;

@Service
@Transactional
public class AutoridadAcademicaServiceImpl implements AutoridadAcademicaService {

    private final AutoridadAcademicaRepository autoridadRepository;
    private final UsuarioRepository usuarioRepository;
    private final InstitucionRepository institucionRepository;
    //    private final IRolAutoridadRepository rolAutoridadRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final DbRoleSyncService dbRoleSyncService;
    private final PostgresProcedureRepository postgresProcedureRepository;
    private final AesCipherService aesCipherService;
    private static final Logger log =
            LoggerFactory.getLogger(AutoridadAcademicaServiceImpl.class);

    public AutoridadAcademicaServiceImpl(
            AutoridadAcademicaRepository autoridadRepository,
            UsuarioRepository usuarioRepository,
            InstitucionRepository institucionRepository,
//            IRolAutoridadRepository rolAutoridadRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            DbRoleSyncService dbRolSyncService,
            PostgresProcedureRepository postgresProcedureRepository, AesCipherService aesCipherService) {
        this.autoridadRepository = autoridadRepository;
        this.usuarioRepository = usuarioRepository;
        this.institucionRepository = institucionRepository;
//        this.rolAutoridadRepository = rolAutoridadRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.dbRoleSyncService = dbRolSyncService;
        this.postgresProcedureRepository = postgresProcedureRepository;
        this.aesCipherService = aesCipherService;
    }

    // -------------------------
    // CRUD "normal"
    // -------------------------
    @Override
    public AutoridadAcademicaResponseDTO crear(AutoridadAcademicaRequestDTO dto) {

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

//        Set<RolAutoridad> cargos = cargarCargos(dto.getIdsRolAutoridad());

        AutoridadAcademica autoridad = new AutoridadAcademica();
        autoridad.setNombres(dto.getNombres());
        autoridad.setApellidos(dto.getApellidos());
        autoridad.setCorreo(dto.getCorreo());
        autoridad.setFechaNacimiento(dto.getFechaNacimiento());
        autoridad.setEstado(dto.getEstado() != null ? dto.getEstado() : true);
        autoridad.setIdInstitucion(dto.getIdInstitucion());
        autoridad.setUsuario(usuario);

        // MULTI CARGOS
//        autoridad.setRolesAutoridad(cargos);

        autoridadRepository.save(autoridad);

        // roles_usuario derivados de TODOS los cargos
//        Set<RolUsuario> rolesDerivados = unirRolesUsuarioDesdeCargos(cargos);

        // Si tu Usuario tiene getRoles() como Set<RolUsuario>
//        usuario.setRoles(new HashSet<>(rolesDerivados));
        usuarioRepository.save(usuario);


        return mapToResponse(autoridad);
    }

    @Override
    public List<AutoridadAcademicaResponseDTO> listar() {
        return autoridadRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AutoridadAcademicaResponseDTO obtenerPorId(Long id) {
        AutoridadAcademica autoridad = autoridadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Autoridad no encontrada"));
        return mapToResponse(autoridad);
    }

    @Override
    public AutoridadAcademicaResponseDTO actualizar(Long id, AutoridadAcademicaRequestDTO dto) {

        AutoridadAcademica autoridad = autoridadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Autoridad no encontrada"));

        Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        autoridad.setNombres(dto.getNombres());
        autoridad.setApellidos(dto.getApellidos());
        autoridad.setCorreo(dto.getCorreo());
        autoridad.setFechaNacimiento(dto.getFechaNacimiento());
        autoridad.setEstado(dto.getEstado());
        autoridad.setUsuario(usuario);
        autoridad.setIdInstitucion(dto.getIdInstitucion());

        // Si vienen cargos, actualiza cargos y roles
        if (dto.getIdsRolAutoridad() != null) {
//            Set<RolAutoridad> cargos = cargarCargos(dto.getIdsRolAutoridad());
////            autoridad.setRolesAutoridad(cargos);
//
//            Set<RolUsuario> rolesDerivados = unirRolesUsuarioDesdeCargos(cargos);

            // Sincronizar roles del usuario según cargos (evita acumulación)
            // Si NO manejas roles extras manuales, lo correcto es reemplazar:
//            usuario.setRoles(new HashSet<>(rolesDerivados));
            usuarioRepository.save(usuario);


            usuarioRepository.save(usuario);
        }

        return mapToResponse(autoridadRepository.save(autoridad));
    }

    @Override
    public void eliminar(Long id) {
        autoridadRepository.deleteById(id);
    }


    @Override
    @Transactional
    public AutoridadRegistroResponseDTO registrarAutoridad(AutoridadRegistroRequestDTO dto) {

        if (dto.getIdInstitucion() == null)
            throw new RuntimeException("idInstitucion es obligatorio");
        if (dto.getRolesApp() == null || dto.getRolesApp().isEmpty())
            throw new RuntimeException("Debe especificar al menos un rolApp");

        institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        // Generar credenciales
        String usuarioApp    = generarUsuarioAppDesdeCorreo(dto.getCorreo());
        String usuarioBd     = generarUsuarioBdUnico(generarUsuarioBdBase(dto.getNombres(), dto.getApellidos()));
        String claveTemporal = generarClaveTemporal(12);
        String claveAppHash  = passwordEncoder.encode(claveTemporal);
        String claveBdReal   = generarClaveTemporal(16);           // ✅ Clave BD dinámica
        String claveBdCifrada = aesCipherService.cifrar(claveBdReal); // ✅ Cifrada con AES

        // ✅ SP hace todo: INSERT usuario + CREATE USER + GRANT roles + INSERT autoridad
        RegistroSpResultDTO resultado = postgresProcedureRepository.registrarAutoridadCompleto(
                usuarioApp, claveAppHash, dto.getCorreo(),
                usuarioBd, claveBdCifrada, claveBdReal,
                dto.getNombres(), dto.getApellidos(), dto.getFechaNacimiento(),
                dto.getIdInstitucion(),
                dto.getRolesApp()  // ✅ Lista de roles
        );
        log.info("✅ Autoridad registrada: {}", usuarioApp);

        // Enviar correo (Spring Boot lo maneja)
        try {
            emailService.enviarCredenciales(dto.getCorreo(), usuarioApp, claveTemporal);
        } catch (Exception ex) {
            log.warn("⚠️ No se pudo enviar correo a {}", dto.getCorreo(), ex);
        }

        AutoridadRegistroResponseDTO resp = new AutoridadRegistroResponseDTO();
        resp.setIdUsuario(resultado.getIdUsuario());
        resp.setIdAutoridad(resultado.getIdAutoridad());
        resp.setUsuarioApp(resultado.getUsuarioApp());
        resp.setUsuarioBd(resultado.getUsuarioBd());
        return resp;
    }

    @Transactional
    public RegistroResponseDTO registrarUsuario(RegistroUsuarioDTO dto) {

        if (usuarioRepository.existsByCorreo(dto.getCorreo()))
            throw new RuntimeException("El correo ya está registrado");

        // usuario_app: parte antes del @ del correo (igual que autoridades)
        // usuario_bd:  misma base normalizada, garantiza unicidad
        String usuarioApp    = generarUsuarioAppDesdeCorreo(dto.getCorreo());
        String usuarioBd     = generarUsuarioBdUnico(usuarioApp); // base = el mismo usuarioApp
        String claveAppPlain = generarClaveTemporal(12);
        String claveBdReal   = generarClaveTemporal(16);
        String claveBdCifrada = aesCipherService.cifrar(claveBdReal);

        // ✅ SP hace todo
        RegistroSpResultDTO resultado = postgresProcedureRepository.registrarUsuarioSimple(
                usuarioApp, passwordEncoder.encode(claveAppPlain), dto.getCorreo(),
                usuarioBd, claveBdCifrada, claveBdReal,
                dto.getRolesApp()  // ✅ Lista de roles
        );
        log.info("✅ Usuario simple registrado: {}", usuarioApp);

        // Enviar correo
        try {
            emailService.enviarCredenciales(dto.getCorreo(), usuarioApp, claveAppPlain);
        } catch (Exception e) {
            log.warn("⚠️ Error enviando correo: {}", e.getMessage());
        }

        return new RegistroResponseDTO(
                "Registro exitoso. Credenciales enviadas a tu correo.",
                dto.getCorreo(), true
        );
    }
    // -------------------------
    // Mapper (incluye cargos)
    // -------------------------
    private AutoridadAcademicaResponseDTO mapToResponse(AutoridadAcademica autoridad) {
        AutoridadAcademicaResponseDTO dto = new AutoridadAcademicaResponseDTO();
        dto.setIdAutoridad(autoridad.getIdAutoridad());
        dto.setNombres(autoridad.getNombres());
        dto.setApellidos(autoridad.getApellidos());
        dto.setCorreo(autoridad.getCorreo());
        dto.setFechaNacimiento(autoridad.getFechaNacimiento());
        dto.setEstado(autoridad.getEstado());
        dto.setIdUsuario(autoridad.getUsuario().getIdUsuario());
        dto.setIdInstitucion(autoridad.getIdInstitucion());

        // ya no existe setRolesAutoridad() → usamos setRolesApp()
        // Mapeamos los roles_app del usuario asociado a la autoridad
        if (autoridad.getUsuario() != null && autoridad.getUsuario().getRolesApp() != null) {
            List<RolAppDTO> rolesApp = autoridad.getUsuario().getRolesApp().stream()
                    .map(r -> {
                        RolAppDTO rdto = new RolAppDTO();
                        rdto.setIdRolApp(r.getIdRolApp());
                        rdto.setNombre(r.getNombre());
                        rdto.setDescripcion(r.getDescripcion());
                        rdto.setActivo(r.getActivo());
                        rdto.setFechaCreacion(r.getFechaCreacion());
                        return rdto;
                    })
                    .sorted(java.util.Comparator.comparing(
                            RolAppDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
                    .collect(java.util.stream.Collectors.toList());
            dto.setRolesApp(rolesApp);
        } else {
            dto.setRolesApp(java.util.Collections.emptyList());
        }

        return dto;
    }
    // -------------------------
    // Helpers generación usuarios
    // -------------------------
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
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ABC.charAt(r.nextInt(ABC.length())));
        return sb.toString();
    }
    @Override
    @Transactional
    public void cambiarEstado(Long idAutoridad, Boolean estado) {
        if (idAutoridad == null) throw new IllegalArgumentException("idAutoridad no puede ser null");
        if (estado == null) throw new IllegalArgumentException("estado no puede ser null");

        AutoridadAcademica a = autoridadRepository.findById(idAutoridad)
                .orElseThrow(() -> new RuntimeException("Autoridad no encontrada: " + idAutoridad));

        a.setEstado(estado);
        autoridadRepository.save(a);
    }

    @Override
    public Long obtenerIdAutoridadPorUsuarioApp(String usuarioApp) {

        if (usuarioApp == null || usuarioApp.isBlank()) {
            throw new RuntimeException("usuarioApp es obligatorio");
        }

        AutoridadAcademica autoridad = autoridadRepository
                .findByUsuario_UsuarioApp(usuarioApp)
                .orElseThrow(() ->
                        new RuntimeException("No existe autoridad para usuarioApp: " + usuarioApp)
                );

        return autoridad.getIdAutoridad();
    }

}
