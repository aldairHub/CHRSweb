package org.uteq.backend.dto;

import lombok.Data;

@Data

public class RolUsuarioResponseDTO {

    private Long idRolUsuario;
    private String nombre;

    public RolUsuarioResponseDTO(Long idRolUsuario, String nombre) {
        this.idRolUsuario = idRolUsuario;
        this.nombre = nombre;
    }

    public RolUsuarioResponseDTO() {

    }
}
