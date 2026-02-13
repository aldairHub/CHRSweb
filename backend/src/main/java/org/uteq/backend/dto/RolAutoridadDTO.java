package org.uteq.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class RolAutoridadDTO {
    private Long idRolAutoridad;
    private String nombre;

    private List<RolUsuarioResponseDTO> rolesUsuario;
}
