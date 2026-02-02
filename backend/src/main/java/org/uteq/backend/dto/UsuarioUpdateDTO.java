package org.uteq.backend.dto;

import org.uteq.backend.Entity.Role;
import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private String usuarioBd;
    private String claveBd;
    private String usuarioApp;
    private String claveApp;
    private Boolean activo;
    private Role rol;
}
