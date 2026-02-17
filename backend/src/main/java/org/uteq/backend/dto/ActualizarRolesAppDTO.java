package org.uteq.backend.dto;

import lombok.Data;
import java.util.List;

/** Payload para actualizar la lista de roles_app de un usuario. */
@Data
public class ActualizarRolesAppDTO {
    /** IDs de los roles_app a asignar (reemplaza lista completa). */
    private List<Integer> idsRolApp;
}