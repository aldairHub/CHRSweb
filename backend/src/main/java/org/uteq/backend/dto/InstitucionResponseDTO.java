package org.uteq.backend.dto;

import lombok.Data;

@Data
public class InstitucionResponseDTO {
    private Long idInstitucion;
    private String nombreInstitucion;
    private String direccion;
    private String correo;
    private String telefono;
    private String logoUrl;
    private String appName;
    private String emailSmtp;
    private String emailHost;
    private Integer emailPort;
    private Boolean emailSsl;
    private Boolean tienePasswordConfigurado;

    private String imagenFondoUrl;

}

