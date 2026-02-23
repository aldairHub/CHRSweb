package org.uteq.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.AutoridadConRolesDTO;
import org.uteq.backend.dto.RolAppDTO;
import org.uteq.backend.dto.UsuarioConRolesDTO;
import org.uteq.backend.entity.AutoridadAcademica;
import org.uteq.backend.entity.RolApp;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.AutoridadAcademicaRepository;
import org.uteq.backend.repository.RolAppRepository;
import org.uteq.backend.repository.UsuarioRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de administración de usuarios y autoridades con roles_app.
 *
 * WHY: Centraliza toda la gestión de roles_app para la pantalla
 * "Gestión de Usuarios" (pestañas Autoridades / Usuarios).
 */
@Service
@Transactional
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolAppRepository rolAppRepository;
    private final AutoridadAcademicaRepository autoridadRepository;
    private final DbRoleSyncService dbRoleSyncService;

    public UsuarioAdminService(UsuarioRepository usuarioRepository,
                               RolAppRepository rolAppRepository,
                               AutoridadAcademicaRepository autoridadRepository,
                               DbRoleSyncService dbRoleSyncService) {
        this.usuarioRepository = usuarioRepository;
        this.rolAppRepository = rolAppRepository;
        this.autoridadRepository = autoridadRepository;
        this.dbRoleSyncService = dbRoleSyncService;
    }

    // ─── Usuarios ──────────────────────────────────────────────

    public List<UsuarioConRolesDTO> listarUsuariosConRoles() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::toUsuarioConRoles)
                .collect(Collectors.toList());
    }

    public void cambiarEstadoUsuario(Long id, Boolean activo) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        u.setActivo(activo);
        usuarioRepository.save(u);
    }

    public UsuarioConRolesDTO actualizarRolesUsuario(Long idUsuario, List<Integer> idsRolApp) {
        Usuario u = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + idUsuario));

        Set<RolApp> nuevosRoles = resolverRoles(idsRolApp);
        u.setRolesApp(nuevosRoles);
        usuarioRepository.save(u);

        return toUsuarioConRoles(u);
    }

    // ─── Autoridades ───────────────────────────────────────────

    public List<AutoridadConRolesDTO> listarAutoridadesConRoles() {
        return autoridadRepository.findAll()
                .stream()
                .map(this::toAutoridadConRoles)
                .collect(Collectors.toList());
    }

    public void cambiarEstadoAutoridad(Long idAutoridad, Boolean estado) {
        AutoridadAcademica a = autoridadRepository.findById(idAutoridad)
                .orElseThrow(() -> new RuntimeException("Autoridad no encontrada: " + idAutoridad));
        a.setEstado(estado);
        autoridadRepository.save(a);
    }

    public AutoridadConRolesDTO actualizarRolesAutoridad(Long idAutoridad, List<Integer> idsRolApp) {
        AutoridadAcademica a = autoridadRepository.findById(idAutoridad)
                .orElseThrow(() -> new RuntimeException("Autoridad no encontrada: " + idAutoridad));

        Usuario u = a.getUsuario();
        Set<RolApp> nuevosRoles = resolverRoles(idsRolApp);
        u.setRolesApp(nuevosRoles);
        usuarioRepository.save(u);

        return toAutoridadConRoles(autoridadRepository.findById(idAutoridad).get());
    }

    // ─── Helpers ───────────────────────────────────────────────

    private Set<RolApp> resolverRoles(List<Integer> idsRolApp) {
        if (idsRolApp == null || idsRolApp.isEmpty()) return new HashSet<>();
        List<RolApp> roles = rolAppRepository.findAllById(idsRolApp);
        return new HashSet<>(roles);
    }

    private UsuarioConRolesDTO toUsuarioConRoles(Usuario u) {
        UsuarioConRolesDTO dto = new UsuarioConRolesDTO();
        dto.setIdUsuario(u.getIdUsuario());
        dto.setUsuarioApp(u.getUsuarioApp());
        dto.setUsuarioBd(u.getUsuarioBd());
        dto.setCorreo(u.getCorreo());
        dto.setActivo(u.getActivo());
        dto.setFechaCreacion(u.getFechaCreacion());
        dto.setRolesApp(toRolAppDTOList(u.getRolesApp()));
        return dto;
    }

    private AutoridadConRolesDTO toAutoridadConRoles(AutoridadAcademica a) {
        AutoridadConRolesDTO dto = new AutoridadConRolesDTO();
        dto.setIdAutoridad(a.getIdAutoridad());
        dto.setNombres(a.getNombres());
        dto.setApellidos(a.getApellidos());
        dto.setCorreo(a.getCorreo());
        dto.setFechaNacimiento(a.getFechaNacimiento());
        dto.setEstado(a.getEstado());
        dto.setIdInstitucion(a.getIdInstitucion());

        Usuario u = a.getUsuario();
        if (u != null) {
            dto.setIdUsuario(u.getIdUsuario());
            dto.setUsuarioApp(u.getUsuarioApp());
            dto.setUsuarioBd(u.getUsuarioBd());
            dto.setRolesApp(toRolAppDTOList(u.getRolesApp()));
        }

        return dto;
    }

    private List<RolAppDTO> toRolAppDTOList(Set<RolApp> roles) {
        if (roles == null) return List.of();
        return roles.stream()
                .map(r -> {
                    RolAppDTO d = new RolAppDTO();
                    d.setIdRolApp(r.getIdRolApp());
                    d.setNombre(r.getNombre());
                    d.setDescripcion(r.getDescripcion());
                    d.setActivo(r.getActivo());
                    d.setFechaCreacion(r.getFechaCreacion());
                    return d;
                })
                .sorted(java.util.Comparator.comparing(RolAppDTO::getNombre,
                        String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }
}