package org.uteq.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AutoridadAcademicaRequestDTO {

    private String nombres;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;
    private Boolean estado;

    private Long idUsuario;
    private Long idInstitucion;
    private Long idFacultad;

    // Múltiples cargos seleccionados (rol_autoridad)
    private List<Long> idsRolAutoridad;
}