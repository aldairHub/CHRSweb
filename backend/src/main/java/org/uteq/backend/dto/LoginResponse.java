package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String usuarioApp;
    private Set<String> roles;
    private Boolean primerLogin;
    private Long idUsuario;
    private ModuloOpcionesDTO modulo;
    private String nombreRolApp;              // nombre legible del RolApp activo
    private List<RolAppDTO> rolesDisponibles; // lista de todos los RolApps del usuario
}
