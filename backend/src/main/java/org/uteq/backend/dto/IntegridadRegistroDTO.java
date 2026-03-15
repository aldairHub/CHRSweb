package org.uteq.backend.dto;

import lombok.Data;

import java.time.OffsetDateTime;
@Data
public class IntegridadRegistroDTO {

    private Long   idAudCambio;
    private OffsetDateTime fecha;
    private String tabla;
    private String operacion;
    private String campo;
    private String usuarioApp;
    private String hashGuardado;
    private String hashCalculado;
    private String estado;          // "OK" | "ALTERADO" | "SIN_HASH"

}
