package org.uteq.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.List;


@Getter @Setter @NoArgsConstructor
public class GenerarDescripcionRequestDTO {

    /** IDs de las solicitudes seleccionadas (el backend extrae sus justificaciones) */
    private List<Long> idsSolicitudes;

    /**
     * Alternativamente, se pueden enviar los textos de justificación directamente
     */
    private List<String> justificaciones;
}