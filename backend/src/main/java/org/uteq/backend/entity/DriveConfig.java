package org.uteq.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Configuración de Google Drive OAuth2 guardada en BD.
 * Los tokens y el client_secret se almacenan cifrados con AES-256.
 * El admin puede configurar esto desde la UI sin tocar application.properties.
 */
@Entity
@Table(name = "drive_config")
@Data
@NoArgsConstructor
public class DriveConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** OAuth2 Client ID — visible, no sensible */
    @Column(name = "client_id", columnDefinition = "TEXT")
    private String clientId;

    /** OAuth2 Client Secret — cifrado AES-256 en BD */
    @Column(name = "client_secret", columnDefinition = "TEXT")
    private String clientSecret;

    /** URI de callback OAuth2 — ej: https://miservidor.com/api/backup/drive/callback */
    @Column(name = "redirect_uri", length = 500)
    private String redirectUri;

    /** Access token cifrado AES-256 */
    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    /** Refresh token cifrado AES-256 (el más importante, permite renovar) */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_type", length = 50)
    private String tokenType = "Bearer";

    /** Cuándo vence el access token */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    /** ID de la carpeta en Drive donde se suben los backups */
    @Column(name = "folder_id")
    private String folderId;

    /** Nombre de la carpeta en Drive */
    @Column(name = "folder_name")
    private String folderName;

    /** true = el admin completó el flujo OAuth2 exitosamente */
    @Column(name = "autorizado", nullable = false)
    private Boolean autorizado = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = Instant.now();
    }
}
