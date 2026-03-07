package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionRequestDTO {

    private Long idProceso;
    private Long idFase;
    private String fecha;
    private String hora;

    @JsonAlias("duracionMinutos")  // acepta tanto "duracion" como "duracionMinutos"
    private Integer duracion;

    private String modalidad;
    private String enlace;
    private List<Long> evaluadoresIds;
    private String observaciones;
}