package org.uteq.backend.dto;

import lombok.Data;

// ── DTO para guardar la configuración de Drive desde la UI ────────
@Data
public class DriveConfigRequestDTO {

    /** OAuth2 Client ID de Google Cloud Console */
    private String clientId;

    /**
     * OAuth2 Client Secret.
     * Si viene vacío, el backend conserva el que ya está en BD.
     * Nunca se devuelve en la respuesta.
     */
    private String clientSecret;

    /**
     * URI de callback OAuth2.
     * Debe coincidir exactamente con la que registraste en Google Cloud Console.
     * Ejemplo: https://tuservidor.com/api/backup/drive/callback
     */
    private String redirectUri;

    /** Nombre de la carpeta en Drive donde se guardarán los backups */
    private String folderName;
}
