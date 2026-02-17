package org.uteq.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ActualizarRolesAppDTO;
import org.uteq.backend.dto.AutoridadConRolesDTO;
import org.uteq.backend.dto.UsuarioConRolesDTO;
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

    public UsuarioAdminController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    // ─── Pestaña USUARIOS ──────────────────────────────────────

    /** Lista todos los usuarios con sus roles_app (excluye los que son autoridades si quieres). */
    @GetMapping("/usuarios")
    public List<UsuarioConRolesDTO> listarUsuarios() {
        return usuarioAdminService.listarUsuariosConRoles();
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
        return ResponseEntity.ok(usuarioAdminService.actualizarRolesUsuario(id, dto.getIdsRolApp()));
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
        return ResponseEntity.ok(usuarioAdminService.actualizarRolesAutoridad(id, dto.getIdsRolApp()));
    }
}