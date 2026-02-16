package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "roles_app_bd",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_rol_app", "nombre_rol_bd"}))
@Data
public class RolAppBd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol_app", nullable = false)
    private RolApp rolApp;

    @Column(name = "nombre_rol_bd", nullable = false, length = 50)
    private String nombreRolBd;

    @Column(name = "fecha_creacion", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
