package org.uteq.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ActualizarRolesAppDTO;
import org.uteq.backend.dto.AutoridadConRolesDTO;
import org.uteq.backend.dto.RegistroUsuarioDTO;
import org.uteq.backend.dto.UsuarioConRolesDTO;
import org.uteq.backend.service.AutoridadAcademicaService;
import org.uteq.backend.service.DbRoleSyncService;
import org.uteq.backend.service.UsuarioAdminService;

import java.util.List;

/**
 * Controlador de administración de usuarios (y autoridades con sus roles_app).
 *
 * Agrupa los endpoints que la nueva pantalla "Gestión de Usuarios" necesita:
 *   - Pestaña Usuarios:    GET /api/admin/usuarios
 *   - Pestaña Autoridades: GET /api/admin/autoridades
 *   - Cambiar estado y roles para ambos tipos
 *
 * WHY 403 antes: Las rutas /api/roles-autoridad y /api/roles-usuario apuntaban
 * a controladores comentados/eliminados → Spring no los registraba → 403/404.
 * Al tener estos endpoints nuevos en SecurityConfig bajo .permitAll() (o con
 * hasAuthority("ADMIN") cuando añadas JWT), el problema desaparece.
 *
 * NOTA PREFIJO ROLE_: Spring Security por defecto antepone "ROLE_" a las
 * authorities si usas .hasRole("ADMIN"). Para evitar eso y poder usar el nombre
 * tal cual (p.ej. "ADMIN"), usa siempre .hasAuthority("ADMIN") en SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;
    private final AutoridadAcademicaService autoridadAcademicaService;
    private final DbRoleSyncService dbRoleSyncService;

    public UsuarioAdminController(UsuarioAdminService usuarioAdminService,
                                  AutoridadAcademicaService autoridadAcademicaService,
                                  DbRoleSyncService dbRoleSyncService) {
        this.usuarioAdminService = usuarioAdminService;
        this.autoridadAcademicaService = autoridadAcademicaService;
        this.dbRoleSyncService = dbRoleSyncService;
    }

    // ─── Pestaña USUARIOS ──────────────────────────────────────

    /** Lista todos los usuarios con sus roles_app (excluye los que son autoridades si quieres). */
    @GetMapping("/usuarios")
    public List<UsuarioConRolesDTO> listarUsuarios() {
        return usuarioAdminService.listarUsuariosConRoles();
    }

    /**
     * Crea un usuario simple desde el panel de administración.
     * Delega a AutoridadAcademicaService.registrarUsuario() que usa
     * sp_registrar_usuario_simple: genera credenciales automáticamente,
     * crea el usuario en PostgreSQL, asigna roles BD y envía correo.
     *
     * Body: { correo, cedula, nombres, apellidos, rolesApp: string[] }
     */
    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody RegistroUsuarioDTO dto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(autoridadAcademicaService.registrarUsuario(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** Activa / desactiva un usuario. */
    @PatchMapping("/usuarios/{id}/estado")
    public ResponseEntity<?> cambiarEstadoUsuario(
            @PathVariable Long id,
            @RequestParam Boolean activo) {
        usuarioAdminService.cambiarEstadoUsuario(id, activo);
        return ResponseEntity.ok().build();
    }

    /** Reemplaza los roles_app de un usuario. */
    @PutMapping("/usuarios/{id}/roles")
    public ResponseEntity<UsuarioConRolesDTO> actualizarRolesUsuario(
            @PathVariable Long id,
            @RequestBody ActualizarRolesAppDTO dto) {
        UsuarioConRolesDTO resultado = usuarioAdminService.actualizarRolesUsuario(id, dto.getIdsRolApp());
        // El @Transactional del service ya hizo commit aquí — el SP lee datos correctos
        dbRoleSyncService.syncRolesUsuarioBd(id.intValue(), true);
        return ResponseEntity.ok(resultado);
    }

    // ─── Pestaña AUTORIDADES ───────────────────────────────────

    /** Lista autoridades académicas con sus roles_app (del usuario relacionado). */
    @GetMapping("/autoridades")
    public List<AutoridadConRolesDTO> listarAutoridades() {
        return usuarioAdminService.listarAutoridadesConRoles();
    }

    /** Activa / desactiva una autoridad (campo estado en autoridad_academica). */
    @PatchMapping("/autoridades/{id}/estado")
    public ResponseEntity<?> cambiarEstadoAutoridad(
            @PathVariable Long id,
            @RequestParam Boolean estado) {
        usuarioAdminService.cambiarEstadoAutoridad(id, estado);
        return ResponseEntity.ok().build();
    }

    /** Reemplaza los roles_app del usuario asociado a una autoridad. */
    @PutMapping("/autoridades/{id}/roles")
    public ResponseEntity<AutoridadConRolesDTO> actualizarRolesAutoridad(
            @PathVariable Long id,
            @RequestBody ActualizarRolesAppDTO dto) {
        AutoridadConRolesDTO resultado = usuarioAdminService.actualizarRolesAutoridad(id, dto.getIdsRolApp());
        // resultado.getIdUsuario() viene del DTO — es el id_usuario de la tabla usuario
        dbRoleSyncService.syncRolesUsuarioBd(resultado.getIdUsuario().intValue(), true);
        return ResponseEntity.ok(resultado);
    }
}