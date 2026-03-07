package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionesResumenDTO {
    private int                   noLeidas;
    private List<NotificacionDTO> notificaciones;
}
