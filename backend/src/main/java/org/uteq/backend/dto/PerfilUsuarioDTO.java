
package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PerfilUsuarioDTO {
    private String usuarioApp;
    private String correo;
    private String fotoPerfil;  // null si no tiene foto
}