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

    // FULL | INCREMENTAL | DIFERENCIAL (tipo real del backup generado)
    @Column(name = "tipo_backup_ext", length = 20)
    private String tipoBackupExt = "FULL";

    // Tipo de la config (FULL | INCREMENTAL | DIFERENCIAL)
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

    // ── Google Drive ──────────────────────────────────────────────
    @Column(name = "drive_file_id")
    private String driveFileId;

    @Column(name = "drive_url")
    private String driveUrl;

    @Column(name = "drive_subido", nullable = false)
    private Boolean driveSubido = false;

    // ── Email ─────────────────────────────────────────────────────
    @Column(name = "email_enviado", nullable = false)
    private Boolean emailEnviado = false;

    // ── LSN (para tracking de incrementales/diferenciales) ────────
    @Column(name = "lsn_fin")
    private String lsnFin;

    @PrePersist
    protected void onCreate() {
        if (fechaInicio == null) fechaInicio = LocalDateTime.now();
    }
}
