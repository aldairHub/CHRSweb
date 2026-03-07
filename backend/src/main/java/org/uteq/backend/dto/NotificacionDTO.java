package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {

    private Long    idNotificacion;
    private String  tipo;           // success | error | warning | info
    private String  titulo;
    private String  mensaje;
    private Boolean leida;
    private String  entidadTipo;
    private Long    entidadId;
    private LocalDateTime fechaCreacion;
    private String  tiempoRelativo; // "Hace 2 horas" — viene del SP
}
