package org.uteq.backend.dto;

import lombok.Data;

@Data
public class RegistroSpResultDTO {
    private Long idUsuario;
    private Long idAutoridad;    // Solo para autoridad, null en los demás
    private String usuarioApp;
    private String usuarioBd;
}