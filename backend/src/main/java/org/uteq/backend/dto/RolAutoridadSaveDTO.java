package org.uteq.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
@Data
public class RolAutoridadSaveDTO {
    private String nombre;
    private List<Long> rolesUsuarioIds;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<Long> getRolesUsuarioIds() { return rolesUsuarioIds; }
    public void setRolesUsuarioIds(List<Long> rolesUsuarioIds) { this.rolesUsuarioIds = rolesUsuarioIds; }
}
