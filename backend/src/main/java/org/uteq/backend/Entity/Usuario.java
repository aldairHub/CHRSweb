package org.uteq.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "usuario_bd", nullable = false)
    private String usuarioBd;

    @Column(name = "clave_bd", nullable = false)
    private String claveBd;

    @Column(name = "usuario_app", nullable = false, unique = true)
    private String usuarioApp;

    @Column(name = "clave_app", nullable = false)
    private String claveApp;

    @Column(name = "correo", nullable = false, unique = true)
    private String correo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

//    @Enumerated(EnumType.STRING)
//    @Column(name = "rol", nullable = false)
//    private Role rol;
}
