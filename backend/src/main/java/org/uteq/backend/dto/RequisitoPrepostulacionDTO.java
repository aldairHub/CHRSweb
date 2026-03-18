package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequisitoPrepostulacionDTO {
    private Long    idRequisito;
    private String  nombre;
    private String  descripcion;
    private Integer orden;
}