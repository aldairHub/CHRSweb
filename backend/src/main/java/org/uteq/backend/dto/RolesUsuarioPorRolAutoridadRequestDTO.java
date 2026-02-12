package org.uteq.backend.dto;

import java.util.List;

public class RolesUsuarioPorRolAutoridadRequestDTO {

    private List<Long> idsRolAutoridad;

    public List<Long> getIdsRolAutoridad() {
        return idsRolAutoridad;
    }

    public void setIdsRolAutoridad(List<Long> idsRolAutoridad) {
        this.idsRolAutoridad = idsRolAutoridad;
    }
}