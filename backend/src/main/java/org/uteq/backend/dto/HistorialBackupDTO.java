package org.uteq.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialBackupDTO {

    private Long          idHistorial;
    private String        estado;
    private String        tipoBackup;
    private String        tipoBackupExt;
    private String        rutaArchivo;
    private Long          tamanoBytes;
    private Long          duracionSegundos;
    private String        mensajeError;
    private String        origen;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;

    // Formateados para la UI
    private String        tamanoFormateado;
    private String        duracionFormateada;

    // Google Drive
    private String        driveFileId;
    private String        driveUrl;
    private Boolean       driveSubido;

    // Email
    private Boolean       emailEnviado;

    // LSN
    private String        lsnFin;
}
