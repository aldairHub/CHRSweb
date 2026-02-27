package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

// ─── REQUEST (mapea exactamente ReunionCreatePayload del frontend) ─────────
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionRequestDTO {

    private Long idPostulante;    // = idProceso en backend
    private Long idFase;
    private String fecha;         // "yyyy-MM-dd"
    private String hora;          // "HH:mm"
    private Integer duracion;
    /** zoom | meet | teams | presencial */
    private String modalidad;
    private String enlace;
    private List<Long> evaluadoresIds;
    private String observaciones;
}
