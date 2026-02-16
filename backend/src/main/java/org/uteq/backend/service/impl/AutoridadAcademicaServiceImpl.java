package org.uteq.backend.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.uteq.backend.entity.AutoridadAcademica;
//import org.uteq.backend.entity.RolAutoridad;
//import org.uteq.backend.entity.RolUsuario;
import org.uteq.backend.entity.Usuario;

import org.uteq.backend.repository.AutoridadAcademicaRepository;
//import org.uteq.backend.repository.IRolAutoridadRepository;
import org.uteq.backend.repository.InstitucionRepository;
import org.uteq.backend.repository.UsuarioRepository;

import org.uteq.backend.service.AutoridadAcademicaService;
import org.uteq.backend.service.DbRoleSyncService;
import org.uteq.backend.service.EmailService;

import org.uteq.backend.dto.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.AutoridadAcademicaResponseDTO;
import org.uteq.backend.dto.AutoridadRegistroRequestDTO;
import org.uteq.backend.dto.AutoridadRegistroResponseDTO;
import org.uteq.backend.dto.RolAutoridadDTO;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log =
            LoggerFactory.getLogger(AutoridadAcademicaServiceImpl.class);

    public AutoridadAcademicaServiceImpl(
            AutoridadAcademicaRepository autoridadRepository,
            UsuarioRepository usuarioRepository,
            InstitucionRepository institucionRepository,
//            IRolAutoridadRepository rolAutoridadRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            DbRoleSyncService dbRolSyncService
    ) {
        this.autoridadRepository = autoridadRepository;
        this.usuarioRepository = usuarioRepository;
        this.institucionRepository = institucionRepository;
//        this.rolAutoridadRepository = rolAutoridadRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.dbRoleSyncService = dbRolSyncService;
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

    // -------------------------
    // Registro (crea Usuario + Autoridad)
    // -------------------------
    @Override
    public AutoridadRegistroResponseDTO registrarAutoridad(AutoridadRegistroRequestDTO dto) {
        if (dto.getIdInstitucion() == null) {
            throw new RuntimeException("idInstitucion es obligatorio");
        }
        if (dto.getIdsRolAutoridad() == null || dto.getIdsRolAutoridad().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos un cargo (idsRolAutoridad)");
        }
        if (dto.getIdsRolAutoridad().stream().anyMatch(Objects::isNull)) {
            throw new RuntimeException("idsRolAutoridad contiene valores nulos");
        }
        institucionRepository.findById(dto.getIdInstitucion())
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

//        Set<RolAutoridad> cargos = cargarCargos(dto.getIdsRolAutoridad());

        // generar usuarioApp (a partir del correo)
        String usuarioApp = generarUsuarioAppDesdeCorreo(dto.getCorreo());

        // generar usuarioBd (a partir de nombres+apellidos, único)
        String baseBd = generarUsuarioBdBase(dto.getNombres(), dto.getApellidos());
        String usuarioBd = generarUsuarioBdUnico(baseBd);

        // clave temporal (solo email), hash en BD
        String claveTemporal = generarClaveTemporal(12);
        String claveHash = passwordEncoder.encode(claveTemporal);

        Usuario usuario = new Usuario();
        usuario.setUsuarioApp(usuarioApp);
        usuario.setClaveApp(claveHash);
        usuario.setCorreo(dto.getCorreo());
        usuario.setUsuarioBd(usuarioBd);

        // Cambiar
        usuario.setClaveBd("MTIzNA==");
        usuario.setActivo(true);

        //  Asignar roles_usuario desde todos los cargos
//        Set<RolUsuario> rolesDerivados = unirRolesUsuarioDesdeCargos(cargos);
//        usuario.getRoles().addAll(rolesDerivados);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        try {
            dbRoleSyncService.  syncRolesUsuarioBd(usuarioGuardado.getIdUsuario().intValue(), false);
            // false: en registro normalmente solo agregas; no revocas nada.
        } catch (Exception ex) {
            // Recomendación: falla para que haga rollback y no quede usuario sin permisos reales
            throw new RuntimeException("Falló la asignación de permisos en BD (sp_sync_roles_usuario_bd): " + ex.getMessage(), ex);
        }

        AutoridadAcademica autoridad = new AutoridadAcademica();
        autoridad.setNombres(dto.getNombres());
        autoridad.setApellidos(dto.getApellidos());
        autoridad.setCorreo(dto.getCorreo());
        autoridad.setFechaNacimiento(dto.getFechaNacimiento());
        autoridad.setEstado(true);
        autoridad.setIdInstitucion(dto.getIdInstitucion());
        autoridad.setUsuario(usuarioGuardado);

        // MULTI CARGOS
//        autoridad.setRolesAutoridad(cargos);

        AutoridadAcademica autoridadGuardada = autoridadRepository.save(autoridad);

        // correo con credenciales
        try {
            emailService.enviarCredenciales(dto.getCorreo(), usuarioApp, claveTemporal);
        } catch (Exception ex) {
            log.warn("No se pudo enviar correo a {}", dto.getCorreo(), ex);
        }
        AutoridadRegistroResponseDTO resp = new AutoridadRegistroResponseDTO();
        resp.setIdUsuario(usuarioGuardado.getIdUsuario());
        resp.setIdAutoridad(autoridadGuardada.getIdAutoridad());
        resp.setUsuarioApp(usuarioGuardado.getUsuarioApp());
        resp.setUsuarioBd(usuarioGuardado.getUsuarioBd());
        return resp;
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

        // cargos (rol_autoridad)
//        List<RolAutoridadDTO> cargos = autoridad.getRolesAutoridad() == null
//                ? Collections.emptyList()
//                : autoridad.getRolesAutoridad().stream().map(r -> {
//                    RolAutoridadDTO rdto = new RolAutoridadDTO();
//                    rdto.setIdRolAutoridad(r.getIdRolAutoridad());
//                    rdto.setNombre(r.getNombre());
//                    return rdto;
//                }).sorted(Comparator.comparing(RolAutoridadDTO::getNombre, String.CASE_INSENSITIVE_ORDER))
//                .collect(Collectors.toList());

        dto.setRolesAutoridad(null);

        return dto;
    }

    // -------------------------
    // Helpers multi-cargos
    // -------------------------
//    private Set<RolAutoridad> cargarCargos(List<Long> idsRolAutoridad) {
//        if (idsRolAutoridad == null || idsRolAutoridad.isEmpty()) {
//            throw new RuntimeException("Debe seleccionar al menos un cargo (rol_autoridad)");
//        }
//
//        List<RolAutoridad> encontrados = rolAutoridadRepository.findAllById(idsRolAutoridad);
//
//        Set<Long> esperados = new HashSet<>(idsRolAutoridad);
//        Set<Long> encontradosIds = encontrados.stream()
//                .map(RolAutoridad::getIdRolAutoridad)
//                .collect(Collectors.toSet());
//
//        if (encontradosIds.size() != esperados.size()) {
//            esperados.removeAll(encontradosIds);
//            throw new RuntimeException("Cargos (rol_autoridad) no encontrados: " + esperados);
//        }
//
//        return new HashSet<>(encontrados);
//    }
//
//    private Set<RolUsuario> unirRolesUsuarioDesdeCargos(Set<RolAutoridad> cargos) {
//        return cargos.stream()
//                .filter(Objects::nonNull)
//                .flatMap(c -> c.getRolesUsuario().stream())
//                .collect(Collectors.toSet());
//    }

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
}
