package org.uteq.backend.dto;

import lombok.Data;

/**
 * DTO de respuesta — nunca expone client_secret ni tokens.
 * Solo datos seguros para mostrar en la UI.
 */
@Data
public class DriveConfigResponseDTO {

    private Long    id;

    /** Client ID (no sensible, puede mostrarse parcialmente) */
    private String  clientIdPreview;   // ej: "123456...apps.googleusercontent.com"

    /** true si clientId + clientSecret están guardados */
    private boolean credencialesGuardadas;

    /** URI de callback registrada */
    private String  redirectUri;

    /** true si el admin completó el flujo OAuth2 y hay refresh_token */
    private boolean autorizado;

    /** Nombre de carpeta en Drive */
    private String  folderName;

    /** ID de carpeta en Drive */
    private String  folderId;

    /** Cuándo expira el access token (solo informativo) */
    private String  expiresAt;

    /** Timestamp de última actualización */
    private String  updatedAt;

    /** URL de autorización lista para abrir — null si no hay credenciales */
    private String  authUrl;
}
