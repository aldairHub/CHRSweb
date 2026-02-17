package org.uteq.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RegistroUsuarioDTO {
    private String correo;
    private String cedula;
    private String nombres;
    private String apellidos;
    private List<String> rolesApp;
}
