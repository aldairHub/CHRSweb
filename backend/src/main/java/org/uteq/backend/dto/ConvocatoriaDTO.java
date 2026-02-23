package org.uteq.backend.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ConvocatoriaDTO {
    private Long idConvocatoria;
    private String titulo;
    private String descripcion;
    private LocalDate fechaPublicacion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String estadoConvocatoria;  // 'abierta' | 'cerrada' | 'cancelada'
}