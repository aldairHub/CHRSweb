package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long idUsuario;
    private String usuarioApp;
    private String nombreRol;
    private Long idRol;
    private String mensaje;
}
