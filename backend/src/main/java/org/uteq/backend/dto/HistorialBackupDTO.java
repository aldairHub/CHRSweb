package org.uteq.backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialBackupDTO {
    private Long          idHistorial;
    private String        estado;
    private String        tipoBackup;
    private String        rutaArchivo;
    private Long          tamanoBytes;
    private Long          duracionSegundos;
    private String        mensajeError;
    private String        origen;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String        tamanoFormateado;
    private String        duracionFormateada;
}
