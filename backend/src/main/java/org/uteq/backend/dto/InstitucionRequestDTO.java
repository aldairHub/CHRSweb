package org.uteq.backend.dto;

import lombok.Data;

@Data
public class InstitucionRequestDTO {
    private String nombreInstitucion;
    private String direccion;
    private String correo;
    private String telefono;
    private String appName;
    private String emailSmtp;
    private String gmailPassword;
    private String emailHost;
    private Integer emailPort;
    // logoUrl se recibe por separado via MultipartFile
}
