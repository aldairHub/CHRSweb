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
    private Boolean emailSsl;   // true = SSL (465) | false = STARTTLS (587)

    private String imagenFondoUrl;


}
