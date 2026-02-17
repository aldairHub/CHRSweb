package org.uteq.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.RolAppDTO;
import org.uteq.backend.dto.RolAppSaveDTO;
import org.uteq.backend.dto.RolAppConRolesBdDTO;
import org.uteq.backend.service.RolAppService;

import java.util.List;

/**
 * Controlador para Gestión General de Roles (roles_app ↔ roles_bd).
 *
 * Reemplaza completamente a RolAutoridadController (comentado/eliminado).
 * Ruta: /api/roles-app
 *
 * En SecurityConfig se declara .permitAll() temporalmente en todas las rutas
 * (así estaba para roles-autoridad). Cuando implementes JWT con ADMIN,
 * cambia esas líneas a hasAuthority("ADMIN") - ver nota en SecurityConfig.java.
 */
@RestController
@RequestMapping("/api/roles-app")
@CrossOrigin(origins = "*")
public class RolAppController {

    private final RolAppService rolAppService;

    public RolAppController(RolAppService rolAppService) {
        this.rolAppService = rolAppService;
    }

    /** Lista todos los roles_app con sus roles_bd asociados. */
    @GetMapping
    public List<RolAppConRolesBdDTO> listar() {
        return rolAppService.listarConRolesBd();
    }

    /** Lista un rol_app por id. */
    @GetMapping("/{id}")
    public ResponseEntity<RolAppConRolesBdDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(rolAppService.obtenerPorId(id));
    }

    /** Lista los roles de BD disponibles (pg_roles con prefijo role_*). */
    @GetMapping("/roles-bd-disponibles")
    public List<String> rolesBdDisponibles() {
        return rolAppService.listarRolesBdDisponibles();
    }

    /** Crea un rol_app con sus mapeos a roles_bd. */
    @PostMapping
    public ResponseEntity<RolAppConRolesBdDTO> crear(@RequestBody RolAppSaveDTO dto) {
        return ResponseEntity.ok(rolAppService.crear(dto));
    }

    /** Actualiza nombre, descripcion y mapeos roles_bd. */
    @PutMapping("/{id}")
    public ResponseEntity<RolAppConRolesBdDTO> actualizar(
            @PathVariable Integer id,
            @RequestBody RolAppSaveDTO dto) {
        return ResponseEntity.ok(rolAppService.actualizar(id, dto));
    }

    /** Activa o desactiva un rol_app. */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam Boolean activo) {
        rolAppService.cambiarEstado(id, activo);
        return ResponseEntity.ok().build();
    }
}