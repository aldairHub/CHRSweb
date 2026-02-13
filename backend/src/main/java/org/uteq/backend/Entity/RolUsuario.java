package org.uteq.backend.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@Table(name = "rol_usuario",
        uniqueConstraints = @UniqueConstraint(name="uq_rol_usuario_nombre", columnNames="nombre"))
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RolUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_usuario")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long idRolUsuario;

    @Column(name = "nombre", nullable = false, length = 100)
    @ToString.Include
    private String nombre;

    // Relación con Usuario (solo si en Usuario el campo se llama "roles")
    @ManyToMany(mappedBy = "roles")
    private Set<Usuario> usuarios = new HashSet<>();

    @ManyToMany(mappedBy = "rolesUsuario")
    private Set<RolAutoridad> rolesAutoridad = new HashSet<>();
}
