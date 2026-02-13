package org.uteq.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class RolAutoridadResponseDTO {
    private Long idRolAutoridad;
    private String nombre;
    private Boolean estado;
    private List<RolUsuarioDTO> rolesUsuario = new ArrayList<>();

    public RolAutoridadResponseDTO(Long idRolAutoridad, String nombre, Boolean estado) {
        this.idRolAutoridad = idRolAutoridad;
        this.nombre = nombre;
        this.estado = estado;
    }

    public Long getIdRolAutoridad() { return idRolAutoridad; }
    public String getNombre() { return nombre; }
    public Boolean getEstado() { return estado; }
    public List<RolUsuarioDTO> getRolesUsuario() { return rolesUsuario; }
}
