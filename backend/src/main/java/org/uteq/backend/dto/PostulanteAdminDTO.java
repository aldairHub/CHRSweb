package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostulanteAdminDTO {
    private Long    idPostulante;
    private String  nombres;
    private String  apellidos;
    private String  identificacion;
    private String  correo;
    private String  usuarioApp;   // usuario de la app (de la tabla usuario)
    private Boolean activo;       // estado del usuario asociado
    private String  fotoPerfil;   // URL foto de perfil (nullable)
}