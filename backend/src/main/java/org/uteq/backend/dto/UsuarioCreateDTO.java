package org.uteq.backend.dto;

import org.uteq.backend.Entity.Role;
import lombok.Data;

@Data
public class UsuarioCreateDTO {
    private String usuarioBd;
    private String claveBd;
    private String usuarioApp;
    private String claveApp;
    private Role rol;
}
