package org.uteq.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO completo de un rol_app con la lista de roles_bd asociados.
 * Usado en la tabla de Gestión de Roles del frontend.
 */
@Data
public class RolAppConRolesBdDTO {
    private Integer idRolApp;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime fechaCreacion;

    /** Nombres de los roles de BD (p.ej. "role_admin_bd", "role_lecturas"). */
    private List<String> rolesBd;
}