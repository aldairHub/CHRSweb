package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "historial_backup")
@Data
@NoArgsConstructor
public class HistorialBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long idHistorial;

    // EXITOSO | FALLIDO
    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    // COMPLETO | INCREMENTAL
    @Column(name = "tipo_backup", length = 20)
    private String tipoBackup;

    @Column(name = "ruta_archivo")
    private String rutaArchivo;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "duracion_segundos")
    private Long duracionSegundos;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    // MANUAL | AUTOMATICO
    @Column(name = "origen", length = 20)
    private String origen = "AUTOMATICO";

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @PrePersist
    protected void onCreate() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
    }
}
