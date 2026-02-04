package org.uteq.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class UsuarioCreateDTO {
    private String usuarioBd;
    private String usuarioApp;
    private String rol;
    // contraseña de la app en texto plano (se hashea con BCrypt)
    private String claveApp;
    private String claveBd;
    private String correo;
    private Boolean activo;
    //private Role rol;
    private List<String> rolesAutoridad;
}
