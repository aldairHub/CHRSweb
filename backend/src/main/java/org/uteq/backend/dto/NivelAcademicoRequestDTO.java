// ── NivelAcademicoRequestDTO.java ─────────────────────────────
package org.uteq.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NivelAcademicoRequestDTO {

    @NotBlank
    @JsonAlias({ "nombre" })
    private String nombre;

    private Integer orden;

    private Boolean estado;
}