package org.uteq.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * CAMBIO: se eliminó el campo `rolesAutoridad: List<RolAutoridadDTO>`
 * (entidad rol_autoridad ya no existe) y se reemplazó por
 * `rolesApp: List<RolAppDTO>` (nuevo modelo de seguridad).
 *
 * Esto resuelve el error de compilación en AutoridadAcademicaServiceImpl:
 *   "cannot find symbol: method setRolesAutoridad(<nulltype>)"
 */
@Data
public class AutoridadAcademicaResponseDTO {

    private Long idAutoridad;
    private String nombres;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;
    private Boolean estado;

    private Long idUsuario;
    private Long idInstitucion;

    // ✅ Ahora apunta a roles_app (reemplaza la lista de rol_autoridad)
    private List<RolAppDTO> rolesApp;
}