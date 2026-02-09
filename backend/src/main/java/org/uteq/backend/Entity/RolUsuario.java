package org.uteq.backend.Entity;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol_usuario",
        uniqueConstraints = @UniqueConstraint(name="uq_rol_usuario_nombre", columnNames="nombre"))
public class RolUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_usuario")
    private Long idRolUsuario;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    // (bidireccional)
    @ManyToMany(mappedBy = "roles")
    private Set<Usuario> usuarios = new HashSet<>();

}