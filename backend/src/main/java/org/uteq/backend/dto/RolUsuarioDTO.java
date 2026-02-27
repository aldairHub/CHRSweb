package org.uteq.backend.dto;

import lombok.Data;

@Data
public class RolUsuarioDTO {
    private Long idRolUsuario;
    private String nombre;
    private Boolean estado;

    public RolUsuarioDTO(Long idRolUsuario, String nombre, Boolean estado) {
        this.idRolUsuario = idRolUsuario;
        this.nombre = nombre;
        this.estado = estado;
    }
}
