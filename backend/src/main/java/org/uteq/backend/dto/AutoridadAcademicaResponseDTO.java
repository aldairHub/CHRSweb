package org.uteq.backend.dto;

import lombok.Data;

import java.time.LocalDate;
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
}
