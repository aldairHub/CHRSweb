package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.service.RestaurarBackupService;

import java.util.Map;

@RestController
@RequestMapping("/api/backup")
@CrossOrigin
@RequiredArgsConstructor
public class RestaurarController {

    private final RestaurarBackupService restaurarService;

    /**
     * Valida un backup sin restaurarlo.
     * Devuelve 200 si es válido, 400 con mensaje si no lo es.
     */
    @PostMapping("/validar")
    public ResponseEntity<Map<String, String>> validar(
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            restaurarService.validarBackup(archivo);
            return ResponseEntity.ok(Map.of(
                "estado", "OK",
                "mensaje", "El backup es válido y pertenece a esta aplicación."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "estado", "INVALIDO",
                "mensaje", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "estado", "ERROR",
                "mensaje", "Error al validar el backup: " + e.getMessage()
            ));
        }
    }

    /**
     * Valida y restaura el backup en la base de datos.
     */
    @PostMapping("/restaurar")
    public ResponseEntity<Map<String, String>> restaurar(
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            restaurarService.restaurar(archivo);
            return ResponseEntity.ok(Map.of(
                "estado", "OK",
                "mensaje", "Base de datos restaurada correctamente."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "estado", "INVALIDO",
                "mensaje", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "estado", "ERROR",
                "mensaje", "Error durante la restauración: " + e.getMessage()
            ));
        }
    }
}
