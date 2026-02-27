package org.uteq.backend.dto;

import lombok.Data;

@Data
public class MateriaResponseDTO {

    private Long idMateria;
    private String nombre;
    private Long idCarrera;
    private String nombreCarrera;
    private Long nivel;

}
