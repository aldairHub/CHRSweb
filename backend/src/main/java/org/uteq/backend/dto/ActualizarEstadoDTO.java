package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para actualizar el estado de una prepostulación
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActualizarEstadoDTO {
    private String estado;        // "APROBADO" o "RECHAZADO"
    private String observaciones; // Comentarios del revisor
    private Long idRevisor;       // ID del usuario que revisa
}