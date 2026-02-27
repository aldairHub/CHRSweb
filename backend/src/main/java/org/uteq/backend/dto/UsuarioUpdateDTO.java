package org.uteq.backend.dto;

import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String usuarioBd;
    private String usuarioApp;
    private String claveApp;
    private String claveBd;
    private String correo;
    private Boolean activo;
}
