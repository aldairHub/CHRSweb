package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostulanteListaDTO {
    private Long      idPostulacion;
    private Long      idPostulante;
    private String    identificacion;
    private String    nombresPostulante;
    private String    apellidosPostulante;
    private String    correoPostulante;
    private String    estadoPostulacion;
    private String    nombreMateria;
    private LocalDateTime fechaPostulacion;
    private Boolean   tieneDocumentos;
}
