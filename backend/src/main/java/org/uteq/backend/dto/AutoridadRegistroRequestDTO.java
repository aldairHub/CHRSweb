package org.uteq.backend.dto;


import lombok.Data;

import java.time.LocalDate;

@Data
public class AutoridadRegistroRequestDTO {
    private String nombres;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;

    private Long idInstitucion;
    private Long idRolAutoridad;   // cargo seleccionado

    private String usuarioApp;

    private String claveApp;
}