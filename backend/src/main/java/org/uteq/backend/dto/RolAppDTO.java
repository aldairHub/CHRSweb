
package org.uteq.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** DTO simple de roles_app (para selects/combos). */
@Data
public class RolAppDTO {
    private Integer idRolApp;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
}