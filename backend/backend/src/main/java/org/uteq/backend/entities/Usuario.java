package org.uteq.backend.entities;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "usuario")
public class Usuario { // Cambié el nombre a PascalCase
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_usuario;

    @Column(name = "usuario_bd")
    private String usuarioBd;

    @Column(name = "clave_bd")
    private String claveBd;

    @Column(name = "usuario_app")
    private String usuarioApp;

    @Column(name = "clave_app")
    private String claveApp;

    @ManyToOne
    @JoinColumn(name = "id_rol", referencedColumnName = "id_rol")
    private Roles roles;

    @Column(name = "activo")
    private Boolean activo;
}