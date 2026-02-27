package org.uteq.backend.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;


@Data
public class AutoridadConRolesDTO {
    private Long idAutoridad;
    private String nombres;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;
    private Boolean estado;
    private Long idInstitucion;

    // Datos del usuario asociado
    private Long idUsuario;
    private String usuarioApp;
    private String usuarioBd;

    /** Roles de aplicación asignados al usuario de la autoridad. */
    private List<RolAppDTO> rolesApp;
}