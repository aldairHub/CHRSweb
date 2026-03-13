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

    // Ruta del ejecutable pg_dump
    @Column(name = "ruta_pgdump", nullable = false)
    private String rutaPgdump = "pg_dump";

    // Ruta local donde se generan los ZIP
    @Column(name = "ruta_origen", nullable = false)
    private String rutaOrigen;

    // COMPLETO | INCREMENTAL
    @Column(name = "tipo_backup", nullable = false, length = 20)
    private String tipoBackup = "COMPLETO";

    // Retención
    @Column(name = "retencion_activa", nullable = false)
    private Boolean retencionActiva = false;

    @Column(name = "dias_retencion", nullable = false)
    private Integer diasRetencion = 7;

    // Horario — 1, 2 o 3 ejecuciones diarias
    @Column(name = "num_ejecuciones", nullable = false)
    private Integer numEjecuciones = 2;

    @Column(name = "hora_backup_1", nullable = false)
    private LocalTime horaBackup1 = LocalTime.of(8, 0);

    @Column(name = "hora_backup_2")
    private LocalTime horaBackup2;

    @Column(name = "hora_backup_3")
    private LocalTime horaBackup3;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // Destino: LOCAL | EMAIL
    @Column(name = "tipo_destino", nullable = false, length = 20)
    private String tipoDestino = "LOCAL";

    // Destino LOCAL — ruta donde se copia el ZIP (pendrive, red, etc)
    @Column(name = "ruta_destino")
    private String rutaDestino;

    // Destino EMAIL
    @Column(name = "email_destino")
    private String emailDestino;

    @Column(name = "notificar_error", nullable = false)
    private Boolean notificarError = true;

    @Column(name = "notificar_exito", nullable = false)
    private Boolean notificarExito = false;
}
