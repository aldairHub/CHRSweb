package org.uteq.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Payload para crear o actualizar un rol_app junto con sus mapeos a roles_bd.
 */
@Data
public class RolAppSaveDTO {
    private String nombre;
    private String descripcion;
    private Boolean activo;

    /** Nombres de roles BD a asociar (reemplaza lista completa). */
    private List<String> rolesBd;
}