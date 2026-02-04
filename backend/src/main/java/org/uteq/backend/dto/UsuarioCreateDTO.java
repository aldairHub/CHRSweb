package org.uteq.backend.dto;

import lombok.Data;

@Data
public class UsuarioCreateDTO {
    private String usuarioBd;
    private String usuarioApp;
    // contraseña de la app en texto plano (se hashea con BCrypt)
    private String claveApp;
    private String claveBd;
    private String correo;
    private Boolean activo;
    //private Role rol;
}
