package org.uteq.backend.dto;

import lombok.Data;

@Data
public class UsuarioDTO {
    private Long idUsuario;
    private String usuarioApp;
    private String usuarioBd;
    private String correo;
    private Boolean activo;
    private Boolean primerLogin;

}
