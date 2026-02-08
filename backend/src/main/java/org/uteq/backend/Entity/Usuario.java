package org.uteq.backend.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_usuario_usuario_app", columnNames = "usuario_app"),
                @UniqueConstraint(name = "uq_usuario_usuario_bd", columnNames = "usuario_bd"),
                @UniqueConstraint(name = "uq_usuario_correo", columnNames = "correo")
        })
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "usuario_app", nullable = false, length = 255)
    private String usuarioApp;

    @Column(name = "clave_app", nullable = false, length = 255)
    private String claveApp;

    @Column(name = "usuario_bd", nullable = false, length = 255)
    private String usuarioBd;

    @Column(name = "clave_bd", nullable = false, length = 255)
    private String claveBd;

    @Column(name = "correo", nullable = false, length = 255)
    private String correo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;


    // Usuario -> Roles (N..M)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_rol",
            joinColumns = @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol_usuario", referencedColumnName = "id_rol_usuario")
    )
    private Set<RolUsuario> roles = new HashSet<>();
}
