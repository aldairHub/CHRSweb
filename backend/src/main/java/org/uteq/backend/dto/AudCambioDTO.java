package org.uteq.backend.dto;

import lombok.Data;

/**
 * DTO de respuesta para GET /api/admin/auditoria/cambios.
 */
@Data
public class AudCambioDTO {
    private Long   idAudCambio;
    private String tabla;
    private Long   idRegistro;
    private String operacion;
    private String campo;
    private String valorAntes;
    private String valorDespues;
    private String usuarioBd;
    private String usuarioApp;
    private String ipCliente;
    private String fecha;
}