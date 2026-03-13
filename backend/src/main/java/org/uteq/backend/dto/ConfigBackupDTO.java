package org.uteq.backend.dto;

import lombok.Data;

@Data
public class ConfigBackupDTO {
    private Long    idConfig;
    private String  rutaPgdump;
    private String  rutaOrigen;
    private String  tipoBackup;
    private Boolean retencionActiva;
    private Integer diasRetencion;
    private Integer numEjecuciones;
    private String  horaBackup1;
    private String  horaBackup2;
    private String  horaBackup3;
    private Boolean activo;
    private String  tipoDestino;
    private String  rutaDestino;
    private String  emailDestino;
    private Boolean notificarError;
    private Boolean notificarExito;
}
