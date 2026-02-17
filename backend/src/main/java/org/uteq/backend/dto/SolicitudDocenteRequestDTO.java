package org.uteq.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.*;
import org.uteq.backend.entity.Usuario;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudDocenteRequestDTO {

    private Usuario usuario;
    @NotNull(message = "La carrera es obligatoria")
    private Long idCarrera;

    @NotNull(message = "La materia es obligatoria")
    private Long idMateria;

    @NotNull(message = "El área de conocimiento es obligatoria")
    private Long idArea;

    @NotNull(message = "La cantidad de docentes es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Long cantidadDocentes;

    @NotBlank(message = "La justificación es obligatoria")
    @Size(min = 20, message = "La justificación debe tener al menos 20 caracteres")
    private String justificacion;

    @NotBlank(message = "El nivel académico es obligatorio")
    private String nivelAcademico;

    @NotNull(message = "La experiencia profesional mínima es obligatoria")
    @Min(value = 0, message = "La experiencia no puede ser negativa")
    private Long experienciaProfesionalMin;

    @NotNull(message = "La experiencia docente mínima es obligatoria")
    @Min(value = 0, message = "La experiencia no puede ser negativa")
    private Long experienciaDocenteMin;

    private String observaciones;
}
