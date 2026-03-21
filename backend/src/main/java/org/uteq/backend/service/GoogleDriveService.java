package org.uteq.backend.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.uteq.backend.dto.DriveConfigRequestDTO;
import org.uteq.backend.dto.DriveConfigResponseDTO;
import org.uteq.backend.entity.DriveConfig;
import org.uteq.backend.repository.DriveConfigRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de Google Drive 100% configurable desde la BD/UI.
 * No depende de application.properties para las credenciales OAuth2.
 *
 * Flujo:
 *  1. Admin guarda Client ID + Client Secret + Redirect URI desde la UI
 *  2. Admin abre la URL de autorización generada
 *  3. Google redirige a /api/backup/drive/callback?code=...
 *  4. El backend intercambia el code por access_token + refresh_token
 *  5. Los tokens se guardan cifrados en BD
 *  6. A partir de ahí, Drive funciona automáticamente con refresh silencioso
 */
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveService.class);
    private static final JsonFactory JSON_FACTORY   = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES        = Collections.singletonList(DriveScopes.DRIVE_FILE);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final DriveConfigRepository    driveConfigRepo;
    private final AesCipherService         aes;
    private final org.uteq.backend.repository.ConfigBackupRepository configBackupRepo;

    // ═══════════════════════════════════════════════════════════════
    // GUARDAR CREDENCIALES DESDE LA UI
    // ═══════════════════════════════════════════════════════════════

    /**
     * Guarda Client ID, Client Secret (cifrado) y Redirect URI en BD.
     * NO toca application.properties.
     * Si el admin deja client_secret vacío, conserva el que ya estaba.
     */
    public DriveConfigResponseDTO guardarCredenciales(DriveConfigRequestDTO dto) {
        DriveConfig cfg = driveConfigRepo.findFirstByActivoTrueOrderByIdAsc()
                .orElse(new DriveConfig());

        if (dto.getClientId() == null || dto.getClientId().isBlank()) {
            throw new IllegalArgumentException("El Client ID no puede estar vacío.");
        }
        if (dto.getRedirectUri() == null || dto.getRedirectUri().isBlank()) {
            throw new IllegalArgumentException("La Redirect URI no puede estar vacía.");
        }

        cfg.setClientId(dto.getClientId().trim());
        cfg.setRedirectUri(dto.getRedirectUri().trim());

        // Solo actualizar client_secret si el admin envió uno nuevo
        if (dto.getClientSecret() != null && !dto.getClientSecret().isBlank()) {
            cfg.setClientSecret(aes.cifrar(dto.getClientSecret().trim()));
        }

        if (dto.getFolderName() != null && !dto.getFolderName().isBlank()) {
            cfg.setFolderName(dto.getFolderName().trim());
        }

        cfg.setActivo(true);
        // Resetear autorización cuando cambian las credenciales
        cfg.setAutorizado(false);
        cfg.setAccessToken(null);
        cfg.setRefreshToken(null);
        cfg.setFolderId(null);

        driveConfigRepo.save(cfg);
        log.info("Credenciales de Google Drive actualizadas desde la UI.");
        return toDTO(cfg);
    }

    // ═══════════════════════════════════════════════════════════════
    // GENERAR URL DE AUTORIZACIÓN
    // ═══════════════════════════════════════════════════════════════

    /**
     * Genera la URL de Google para que el admin autorice el acceso.
     * Requiere que ya estén guardados clientId + clientSecret + redirectUri.
     */
    public String generarUrlAutorizacion() throws Exception {
        DriveConfig cfg = obtenerConfigOrThrow();
        validarCredenciales(cfg);

        String clientId     = cfg.getClientId();
        String clientSecret = aes.descifrar(cfg.getClientSecret());
        String redirectUri  = cfg.getRedirectUri();

        return new GoogleAuthorizationCodeFlow
                .Builder(buildTransport(), JSON_FACTORY, clientId, clientSecret, SCOPES)
                .setAccessType("offline")       // ← CRÍTICO: necesario para obtener refresh_token
                .setApprovalPrompt("force")     // ← CRÍTICO: fuerza a mostrar pantalla de permisos
                .build()
                .newAuthorizationUrl()
                .setRedirectUri(redirectUri)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // CALLBACK — intercambiar code por tokens
    // ═══════════════════════════════════════════════════════════════

    /**
     * Llamado por el controller cuando Google redirige al callback con ?code=...
     * Intercambia el código por access_token + refresh_token y los cifra en BD.
     */
    public void procesarCallback(String code) throws Exception {
        DriveConfig cfg = obtenerConfigOrThrow();
        validarCredenciales(cfg);

        String clientId     = cfg.getClientId();
        String clientSecret = aes.descifrar(cfg.getClientSecret());
        String redirectUri  = cfg.getRedirectUri();

        GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                buildTransport(), JSON_FACTORY,
                clientId, clientSecret, code, redirectUri
        ).execute();

        // Cifrar y guardar tokens
        cfg.setAccessToken(aes.cifrar(response.getAccessToken()));
        cfg.setRefreshToken(aes.cifrar(response.getRefreshToken()));
        cfg.setTokenType(response.getTokenType() != null ? response.getTokenType() : "Bearer");
        cfg.setScope(response.getScope());
        cfg.setExpiresAt(Instant.now().plusSeconds(
                response.getExpiresInSeconds() != null ? response.getExpiresInSeconds() : 3600L));
        cfg.setAutorizado(true);

        // ── CRÍTICO: resetear folder_id al cambiar de cuenta ──────────────────
        // El folder_id anterior pertenece a la cuenta vieja.
        // Al autorizar con una cuenta nueva, hay que crear la carpeta de nuevo
        // en el Drive del nuevo usuario.
        cfg.setFolderId(null);
        driveConfigRepo.save(cfg);

        // También limpiar el folder_id en config_backup para que BackupService
        // lo re-cree en el Drive correcto en el próximo backup
        configBackupRepo.findFirstByOrderByIdConfigAsc().ifPresent(configBackup -> {
            configBackup.setDriveFolderId(null);
            configBackupRepo.save(configBackup);
            log.info("folder_id de config_backup reseteado — se creará en la nueva cuenta de Drive.");
        });

        log.info("Google Drive autorizado correctamente. Tokens guardados cifrados en BD.");
    }

    // ═══════════════════════════════════════════════════════════════
    // ESTADO
    // ═══════════════════════════════════════════════════════════════

    public DriveConfigResponseDTO obtenerEstado() {
        Optional<DriveConfig> opt = driveConfigRepo.findFirstByActivoTrueOrderByIdAsc();
        if (opt.isEmpty()) {
            return estadoVacio();
        }
        return toDTO(opt.get());
    }

    public boolean estaListo() {
        return driveConfigRepo.findFirstByActivoTrueOrderByIdAsc()
                .map(cfg -> tieneCredenciales(cfg) && Boolean.TRUE.equals(cfg.getAutorizado()))
                .orElse(false);
    }

    // ═══════════════════════════════════════════════════════════════
    // REVOCAR
    // ═══════════════════════════════════════════════════════════════

    public void revocarAutorizacion() {
        driveConfigRepo.findFirstByActivoTrueOrderByIdAsc().ifPresent(cfg -> {
            cfg.setAccessToken(null);
            cfg.setRefreshToken(null);
            cfg.setAutorizado(false);
            cfg.setFolderId(null);
            driveConfigRepo.save(cfg);
        });
        log.info("Autorización de Google Drive revocada.");
    }

    /** Borrar TODO: credenciales + tokens */
    public void eliminarConfig() {
        driveConfigRepo.findFirstByActivoTrueOrderByIdAsc().ifPresent(cfg -> {
            cfg.setClientId(null);
            cfg.setClientSecret(null);
            cfg.setRedirectUri(null);
            cfg.setAccessToken(null);
            cfg.setRefreshToken(null);
            cfg.setAutorizado(false);
            cfg.setFolderId(null);
            cfg.setFolderName(null);
            driveConfigRepo.save(cfg);
        });
        log.info("Configuración de Google Drive eliminada completamente.");
    }

    // ═══════════════════════════════════════════════════════════════
    // SUBIR ARCHIVO (uso interno del BackupService)
    // ═══════════════════════════════════════════════════════════════

    public DriveUploadResult subirArchivo(java.io.File archivoLocal,
                                          String folderId,
                                          String mimeType) throws Exception {
        final int MAX = 3;
        Exception ultimo = null;
        for (int i = 1; i <= MAX; i++) {
            try {
                Drive drive = buildDriveClient();
                File meta   = new File();
                meta.setName(archivoLocal.getName());
                if (folderId != null && !folderId.isBlank()) {
                    meta.setParents(Collections.singletonList(folderId));
                }
                File uploaded = drive.files()
                        .create(meta, new FileContent(mimeType, archivoLocal))
                        .setFields("id,name,webViewLink")
                        .execute();
                log.info("Archivo subido a Drive (intento {}): {}", i, uploaded.getId());
                return new DriveUploadResult(uploaded.getId(), uploaded.getWebViewLink());
            } catch (Exception e) {
                ultimo = e;
                log.warn("Drive upload intento {}/{}: {}", i, MAX, e.getMessage());
                if (i < MAX) Thread.sleep(2000L * i);
            }
        }
        throw new RuntimeException("Error al subir a Drive tras " + MAX + " intentos: "
                + (ultimo != null ? ultimo.getMessage() : ""), ultimo);
    }

    // ═══════════════════════════════════════════════════════════════
    // OBTENER O CREAR CARPETA EN DRIVE
    // ═══════════════════════════════════════════════════════════════

    public String obtenerOCrearCarpeta(String nombre) throws Exception {
        Drive drive = buildDriveClient();
        String q    = "mimeType='application/vnd.google-apps.folder'"
                + " and name='" + nombre.replace("'", "\\'") + "'"
                + " and trashed=false";
        FileList result = drive.files().list().setQ(q).setFields("files(id,name)").execute();

        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            String id = result.getFiles().get(0).getId();
            persistirFolderId(nombre, id);
            return id;
        }

        File carpeta = new File();
        carpeta.setName(nombre);
        carpeta.setMimeType("application/vnd.google-apps.folder");
        File creada = drive.files().create(carpeta).setFields("id").execute();
        log.info("Carpeta creada en Drive: {} ({})", nombre, creada.getId());
        persistirFolderId(nombre, creada.getId());
        return creada.getId();
    }

    // ═══════════════════════════════════════════════════════════════
    // PRIVADOS
    // ═══════════════════════════════════════════════════════════════

    private Drive buildDriveClient() throws Exception {
        NetHttpTransport transport = buildTransport();
        Credential credential     = obtenerCredencialRefrescada(transport);
        DriveConfig cfg           = obtenerConfigOrThrow();
        return new Drive.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName("SSDC-Backup")
                .build();
    }

    private Credential obtenerCredencialRefrescada(NetHttpTransport transport) throws Exception {
        DriveConfig cfg = obtenerConfigOrThrow();

        if (!Boolean.TRUE.equals(cfg.getAutorizado()) || cfg.getRefreshToken() == null) {
            throw new RuntimeException("Google Drive no está autorizado. El admin debe completar el flujo OAuth2.");
        }

        String clientId      = cfg.getClientId();
        String clientSecret  = aes.descifrar(cfg.getClientSecret());
        String accessToken   = cfg.getAccessToken()  != null ? aes.descifrar(cfg.getAccessToken())  : null;
        String refreshToken  = aes.descifrar(cfg.getRefreshToken());

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken);

        // Refrescar si expira en < 5 minutos o no hay access token
        boolean necesitaRefrescar = accessToken == null
                || cfg.getExpiresAt() == null
                || Instant.now().isAfter(cfg.getExpiresAt().minusSeconds(300));

        if (necesitaRefrescar) {
            log.info("Refrescando access token de Google Drive...");
            if (!credential.refreshToken()) {
                throw new RuntimeException("No se pudo refrescar el token de Drive. El admin debe reautorizar.");
            }
            // Guardar nuevo access token cifrado
            cfg.setAccessToken(aes.cifrar(credential.getAccessToken()));
            cfg.setExpiresAt(Instant.now().plusSeconds(
                    credential.getExpiresInSeconds() != null ? credential.getExpiresInSeconds() : 3600L));
            driveConfigRepo.save(cfg);
            log.info("Access token de Drive refrescado y guardado en BD.");
        }
        return credential;
    }

    private DriveConfig obtenerConfigOrThrow() {
        return driveConfigRepo.findFirstByActivoTrueOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException(
                        "No hay configuración de Google Drive. Configúrala desde la UI."));
    }

    private void validarCredenciales(DriveConfig cfg) {
        if (!tieneCredenciales(cfg)) {
            throw new IllegalStateException(
                    "Faltan credenciales de Google Drive. Guarda Client ID, Client Secret y Redirect URI primero.");
        }
    }

    private boolean tieneCredenciales(DriveConfig cfg) {
        return cfg.getClientId() != null && !cfg.getClientId().isBlank()
                && cfg.getClientSecret() != null && !cfg.getClientSecret().isBlank()
                && cfg.getRedirectUri() != null && !cfg.getRedirectUri().isBlank();
    }

    private void persistirFolderId(String nombre, String id) {
        driveConfigRepo.findFirstByActivoTrueOrderByIdAsc().ifPresent(c -> {
            c.setFolderName(nombre);
            c.setFolderId(id);
            driveConfigRepo.save(c);
        });
    }

    private NetHttpTransport buildTransport() {
        try {
            return GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            throw new RuntimeException("Error al crear transporte HTTPS: " + e.getMessage(), e);
        }
    }

    private DriveConfigResponseDTO toDTO(DriveConfig cfg) {
        DriveConfigResponseDTO dto = new DriveConfigResponseDTO();
        dto.setId(cfg.getId());
        dto.setCredencialesGuardadas(tieneCredenciales(cfg));
        dto.setAutorizado(Boolean.TRUE.equals(cfg.getAutorizado()));
        dto.setRedirectUri(cfg.getRedirectUri());
        dto.setFolderName(cfg.getFolderName());
        dto.setFolderId(cfg.getFolderId());

        // Preview del Client ID (no exponemos el secret)
        if (cfg.getClientId() != null) {
            String cid = cfg.getClientId();
            dto.setClientIdPreview(cid.length() > 20
                    ? cid.substring(0, 12) + "..." + cid.substring(cid.length() - 8)
                    : cid);
        }

        if (cfg.getExpiresAt() != null) {
            dto.setExpiresAt(FORMATTER.format(cfg.getExpiresAt()));
        }
        if (cfg.getUpdatedAt() != null) {
            dto.setUpdatedAt(FORMATTER.format(cfg.getUpdatedAt()));
        }

        // Generar URL de auth solo si hay credenciales
        if (tieneCredenciales(cfg)) {
            try {
                dto.setAuthUrl(generarUrlAutorizacion());
            } catch (Exception e) {
                log.debug("No se pudo generar authUrl: {}", e.getMessage());
            }
        }
        return dto;
    }

    private DriveConfigResponseDTO estadoVacio() {
        DriveConfigResponseDTO dto = new DriveConfigResponseDTO();
        dto.setCredencialesGuardadas(false);
        dto.setAutorizado(false);
        return dto;
    }

    // ── Records de resultado ─────────────────────────────────────────
    public record DriveUploadResult(String fileId, String webViewLink) {}
}