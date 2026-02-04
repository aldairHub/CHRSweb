package org.uteq.backend.dto;

import lombok.Data;

@Data
public class UsuarioDTO {
    private Long idUsuario;
    private String usuarioBd;
    private String usuarioApp;
    private Boolean activo;
    //private Role rol;
}
