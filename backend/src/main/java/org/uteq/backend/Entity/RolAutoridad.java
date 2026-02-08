package org.uteq.backend.Entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
@Data
@Entity
@Table(name = "rol_autoridad",
        uniqueConstraints = @UniqueConstraint(name="uq_rol_autoridad_nombre", columnNames="nombre"))
public class RolAutoridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_autoridad")
    private Long idRolAutoridad;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // Cargo -> RolesUsuario (N..M)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rol_autoridad_rol_usuario",
            joinColumns = @JoinColumn(name = "id_rol_autoridad", referencedColumnName = "id_rol_autoridad"),
            inverseJoinColumns = @JoinColumn(name = "id_rol_usuario", referencedColumnName = "id_rol_usuario")
    )
    private Set<RolUsuario> rolesUsuario = new HashSet<>();
}