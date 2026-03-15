package org.uteq.backend.dto;

import lombok.Data;

import java.util.List;
@Data
public class IntegridadResumenDTO {

    private int totalVerificados;
    private int totalOk;
    private int totalAlterados;
    private int totalSinHash;
    private List<IntegridadRegistroDTO> registrosSospechosos;


}
