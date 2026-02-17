package org.uteq.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de usuario con sus roles_app.
 * Usado en la pestaña "Usuarios" de Gestión de Usuarios.
 */
@Data
public class UsuarioConRolesDTO {
    private Long idUsuario;
    private String usuarioApp;
    private String usuarioBd;
    private String correo;
    private Boolean activo;
    private LocalDateTime fechaCreacion;

    /** Roles de aplicación asignados. */
    private List<RolAppDTO> rolesApp;
}