//package org.uteq.backend.dto;
//
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//public class PrepostulacionResponseDTO {
//    private String mensaje;
//    private String correo;
//    private String usuarioApp;
//    private boolean exitoso;
//    private Long idPrepostulacion;
//}
package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrepostulacionResponseDTO {
    private String mensaje;
    private String correo;
    private Long id;
    private Boolean exito;
    private LocalDateTime fechaCreacion;
}