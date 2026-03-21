package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "config_backup")
@Data
@NoArgsConstructor
public class ConfigBackup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_config")
    private Long idConfig;

    @Column(name = "ruta_pgdump", nullable = false)
    private String rutaPgdump = "pg_dump";

    @Column(name = "ruta_origen", nullable = false)
    private String rutaOrigen;

    @Column(name = "tipo_backup", nullable = false, length = 20)
    private String tipoBackup = "FULL";

    @Column(name = "retencion_activa", nullable = false)
    private Boolean retencionActiva = false;

    @Column(name = "dias_retencion", nullable = false)
    private Integer diasRetencion = 7;

    @Column(name = "num_ejecuciones", nullable = false)
    private Integer numEjecuciones = 1;

    @Column(name = "hora_backup_1", nullable = false)
    private LocalTime horaBackup1 = LocalTime.of(8, 0);

    @Column(name = "hora_backup_2")
    private LocalTime horaBackup2;

    @Column(name = "hora_backup_3")
    private LocalTime horaBackup3;

    @Column(name = "activo", nullable = false)
    private Boolean activo = false;

    // ── Destinos múltiples ────────────────────────────────────────
    @Column(name = "tipo_destino", nullable = false, length = 20)
    private String tipoDestino = "NINGUNO";

    // Destino LOCAL
    @Column(name = "destino_local", nullable = false)
    private Boolean destinoLocal = false;

    @Column(name = "ruta_destino")
    private String rutaDestino;

    // Destino EMAIL
    @Column(name = "destino_email", nullable = false)
    private Boolean destinoEmail = false;

    @Column(name = "email_destino")
    private String emailDestino;

    // Destino GOOGLE DRIVE
    @Column(name = "destino_drive", nullable = false)
    private Boolean destinoDrive = false;

    @Column(name = "drive_folder_name")
    private String driveFolderName;

    @Column(name = "drive_folder_id")
    private String driveFolderId;

    // ── Notificaciones ────────────────────────────────────────────
    @Column(name = "notificar_error", nullable = false)
    private Boolean notificarError = true;

    @Column(name = "notificar_exito", nullable = false)
    private Boolean notificarExito = false;
}
