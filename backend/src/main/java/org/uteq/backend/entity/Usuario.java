package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_usuario_usuario_app", columnNames = "usuario_app"),
                @UniqueConstraint(name = "uq_usuario_usuario_bd", columnNames = "usuario_bd"),
                @UniqueConstraint(name = "uq_usuario_correo", columnNames = "correo")
        })
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
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

    @Column(name = "fecha_creacion", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaCreacion;

    @Column(name = "primer_login", nullable = false)
    private Boolean primerLogin = true;
    // Relación N:N con roles de aplicación
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "usuario_roles_app",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_rol_app")
    )
    private Set<RolApp> rolesApp = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
