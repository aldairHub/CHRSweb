package org.uteq.backend.dto;

import lombok.Data;

@Data
public class AutoridadRegistroResponseDTO {
    private Long idUsuario;
    private Long idAutoridad;
    private String usuarioApp;
    private String usuarioBd;
}

