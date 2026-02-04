package org.uteq.backend.dto;

import lombok.Data;

@Data
public class RegistroUsuarioDTO {
    private String correo;
    private String cedula;
    private String nombres;
    private String apellidos;
}
