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
@Table(name = "rol_autoridad",
        uniqueConstraints = @UniqueConstraint(name="uq_rol_autoridad_nombre", columnNames="nombre"))
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RolAutoridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_autoridad")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long idRolAutoridad;

    @Column(name = "nombre", nullable = false, length = 150)
    @ToString.Include
    private String nombre;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "rol_autoridad_rol_usuario",
            joinColumns = @JoinColumn(name = "id_rol_autoridad", referencedColumnName = "id_rol_autoridad"),
            inverseJoinColumns = @JoinColumn(name = "id_rol_usuario", referencedColumnName = "id_rol_usuario")
    )
    private Set<RolUsuario> rolesUsuario = new HashSet<>();
}
