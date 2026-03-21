package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ConfigBackupDTO;
import org.uteq.backend.dto.DriveConfigRequestDTO;
import org.uteq.backend.dto.DriveConfigResponseDTO;
import org.uteq.backend.dto.HistorialBackupDTO;
import org.uteq.backend.service.BackupService;
import org.uteq.backend.service.GoogleDriveService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@CrossOrigin
@RequiredArgsConstructor
public class BackupController {

    private final BackupService      backupService;
    private final GoogleDriveService driveService;

    // ═══════════════════════════════════════════════════════════════
    // CONFIGURACIÓN BACKUP
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/config")
    public ResponseEntity<ConfigBackupDTO> obtenerConfig() {
        return ResponseEntity.ok(backupService.obtenerConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<ConfigBackupDTO> guardarConfig(@RequestBody ConfigBackupDTO dto) {
        return ResponseEntity.ok(backupService.guardarConfig(dto));
    }

    // ═══════════════════════════════════════════════════════════════
    // HISTORIAL
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/historial")
    public ResponseEntity<List<HistorialBackupDTO>> obtenerHistorial() {
        return ResponseEntity.ok(backupService.obtenerHistorial());
    }

    // ═══════════════════════════════════════════════════════════════
    // EJECUTAR BACKUP
    // ═══════════════════════════════════════════════════════════════

    /** Backward-compatible: FULL manual */
    @PostMapping("/ejecutar")
    public ResponseEntity<HistorialBackupDTO> ejecutarManual() {
        return ResponseEntity.ok(backupService.ejecutarBackupManual());
    }

    /** Tipo específico: FULL | INCREMENTAL | DIFERENCIAL */
    @PostMapping("/ejecutar/{tipo}")
    public ResponseEntity<?> ejecutarTipo(@PathVariable String tipo) {
        String t = tipo.toUpperCase();
        if (!List.of("FULL", "INCREMENTAL", "DIFERENCIAL").contains(t)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Tipo inválido. Usa: FULL, INCREMENTAL, DIFERENCIAL"));
        }
        return ResponseEntity.ok(backupService.ejecutarBackupTipo(t));
    }

    // ═══════════════════════════════════════════════════════════════
    // GOOGLE DRIVE — CONFIGURACIÓN DESDE LA UI
    // ═══════════════════════════════════════════════════════════════

    /**
     * GET /api/backup/drive/config
     * Devuelve el estado actual de la configuración de Drive.
     * NUNCA expone client_secret ni tokens.
     */
    @GetMapping("/drive/config")
    public ResponseEntity<DriveConfigResponseDTO> obtenerDriveConfig() {
        return ResponseEntity.ok(driveService.obtenerEstado());
    }

    /**
     * POST /api/backup/drive/config
     * Guarda Client ID, Client Secret y Redirect URI en BD (client_secret cifrado AES).
     * El admin usa este endpoint desde la UI.
     */
    @PostMapping("/drive/config")
    public ResponseEntity<?> guardarDriveConfig(@RequestBody DriveConfigRequestDTO dto) {
        try {
            DriveConfigResponseDTO resultado = driveService.guardarCredenciales(dto);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al guardar configuración: " + e.getMessage()));
        }
    }

    /**
     * GET /api/backup/drive/auth-url
     * Genera la URL de autorización OAuth2 de Google.
     * El frontend la abre en una ventana nueva.
     * Requiere que ya estén guardadas las credenciales.
     */
    @GetMapping("/drive/auth-url")
    public ResponseEntity<?> getDriveAuthUrl() {
        try {
            String url = driveService.generarUrlAutorizacion();
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al generar URL de autorización: " + e.getMessage()));
        }
    }

    /**
     * GET /api/backup/drive/callback?code=...
     * Google redirige aquí después de que el admin autoriza.
     * Intercambia el code por tokens y los guarda cifrados en BD.
     *
     * IMPORTANTE: Esta URI debe estar registrada en Google Cloud Console
     * como "Authorized redirect URI".
     */
    @GetMapping("/drive/callback")
    public ResponseEntity<?> driveCallback(@RequestParam String code) {
        try {
            driveService.procesarCallback(code);
            // Devolver HTML con mensaje de éxito para cerrar la ventana popup
            String html = """
                    <!DOCTYPE html>
                    <html lang="es">
                    <head><meta charset="UTF-8"><title>Drive conectado</title>
                    <style>
                      body{font-family:-apple-system,sans-serif;display:flex;align-items:center;
                           justify-content:center;height:100vh;margin:0;background:#f0fdf4}
                      .box{text-align:center;padding:40px;background:#fff;border-radius:16px;
                           box-shadow:0 4px 24px rgba(0,0,0,.1)}
                      .check{font-size:48px;margin-bottom:16px}
                      h2{color:#166534;margin:0 0 8px}p{color:#6b7280;margin:0 0 20px}
                      button{background:#00A63E;color:#fff;border:none;border-radius:8px;
                             padding:10px 24px;font-size:14px;cursor:pointer;font-weight:600}
                    </style></head>
                    <body>
                    <div class="box">
                      <div class="check">✅</div>
                      <h2>Google Drive conectado</h2>
                      <p>La autorización fue exitosa. Puedes cerrar esta ventana.</p>
                      <button onclick="window.close()">Cerrar ventana</button>
                    </div>
                    <script>
                      // Notificar a la ventana padre que la auth fue exitosa
                      if(window.opener){ window.opener.postMessage('drive_auth_ok','*'); }
                      setTimeout(()=>window.close(), 3000);
                    </script>
                    </body></html>
                    """;
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(html);
        } catch (Exception e) {
            log.error("Error en callback de Drive: {}", e.getMessage());
            String htmlError = """
                    <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
                    <title>Error</title>
                    <style>body{font-family:sans-serif;display:flex;align-items:center;
                    justify-content:center;height:100vh;background:#fff1f2}
                    .box{text-align:center;padding:40px;background:#fff;border-radius:16px}
                    h2{color:#991b1b}</style></head>
                    <body><div class="box"><div style="font-size:48px">❌</div>
                    <h2>Error al conectar</h2><p>""" + e.getMessage() + """
                    </p><button onclick="window.close()">Cerrar</button></div></body></html>
                    """;
            return ResponseEntity.internalServerError()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(htmlError);
        }
    }

    /**
     * DELETE /api/backup/drive/autorizar
     * Revoca solo los tokens (mantiene Client ID/Secret).
     * El admin puede reautorizar sin reingresar las credenciales.
     */
    @DeleteMapping("/drive/autorizar")
    public ResponseEntity<Map<String, String>> revocarAutorizacion() {
        driveService.revocarAutorizacion();
        return ResponseEntity.ok(Map.of("mensaje", "Autorización revocada. Las credenciales se mantienen."));
    }

    /**
     * DELETE /api/backup/drive/config
     * Elimina TODA la configuración de Drive (credenciales + tokens).
     */
    @DeleteMapping("/drive/config")
    public ResponseEntity<Map<String, String>> eliminarConfig() {
        driveService.eliminarConfig();
        return ResponseEntity.ok(Map.of("mensaje", "Configuración de Google Drive eliminada."));
    }

    // Logger para el callback
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(BackupController.class);
}
