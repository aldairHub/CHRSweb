package org.uteq.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AutoridadRegistroRequestDTO {

    private String nombres;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;

    private Long idInstitucion;
    private List<String> rolesApp;
    // Cargos seleccionados (rol_autoridad)
    private List<Long> idsRolAutoridad;
}
