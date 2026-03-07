package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Data
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    // 'success' | 'error' | 'warning' | 'info'
    @Column(name = "tipo", nullable = false, length = 10)
    private String tipo = "info";

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @Column(name = "leida", nullable = false)
    private Boolean leida = false;

    // Objeto que originó la notificación (ej: "PREPOSTULACION", "REUNION")
    @Column(name = "entidad_tipo", length = 50)
    private String entidadTipo;

    // ID del objeto que originó la notificación
    @Column(name = "entidad_id")
    private Long entidadId;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_leida")
    private LocalDateTime fechaLeida;

    @PrePersist
    protected void onCreate() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
}
