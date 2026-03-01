package org.uteq.backend.dto;

import lombok.Data;

import java.util.List;

@Data
public class ModuloOpcionesDTO {
    private String          moduloNombre;
    private String          moduloRuta;
    private List<OpcionDTO> opciones;
}
