package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "institucion", schema = "public")
public class Institucion {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_institucion")
    private Long idInstitucion;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "direccion")
    private String direccion;

    @Column(name = "correo")
    private String correo;

    @Column(name = "telefono")
    private String telefono;

    // Campos extra
    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "escudo_url", columnDefinition = "TEXT")
    private String escudoUrl;

    @Column(name = "email_smtp")
    private String emailSmtp;

    @Column(name = "email_password", columnDefinition = "TEXT")
    private String emailPassword;  // almacenado cifrado

    @Column(name = "email_host")
    private String emailHost;

    @Column(name = "email_port")
    private Integer emailPort;

    @Column(name = "app_name")
    private String appName;

    @Column(name = "nombre_corto")
    private String nombreCorto;

    @Column(name = "activo")
    private Boolean activo = true;

     //true  → SSL puro  (puerto recomendado: 465)
    //false → STARTTLS   (puerto recomendado: 587)

    @Column(name = "email_ssl", nullable = false)
    private Boolean emailSsl = false;

    @Column(name = "imagen_fondo_url", columnDefinition = "TEXT")
    private String imagenFondoUrl;
}
