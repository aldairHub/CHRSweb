package org.uteq.backend.dto;

import lombok.Data;

@Data
public class AutoridadRegistroResponseDTO {

    private Long idUsuario;
    private Long idAutoridad;

    // Se puede devolver para informar al admin
    private String usuarioApp;
    private String usuarioBd;

    // Opcional
    // private Boolean correoEnviado;
}
