package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.ConfigBackupDTO;
import org.uteq.backend.dto.HistorialBackupDTO;
import org.uteq.backend.service.BackupService;

import java.util.List;

@RestController
@RequestMapping("/api/backup")
@CrossOrigin
@RequiredArgsConstructor
public class BackupController {

    private final BackupService backupService;

    @GetMapping("/config")
    public ResponseEntity<ConfigBackupDTO> obtenerConfig() {
        return ResponseEntity.ok(backupService.obtenerConfig());
    }

    @PutMapping("/config")
    public ResponseEntity<ConfigBackupDTO> guardarConfig(@RequestBody ConfigBackupDTO dto) {
        return ResponseEntity.ok(backupService.guardarConfig(dto));
    }

    @GetMapping("/historial")
    public ResponseEntity<List<HistorialBackupDTO>> obtenerHistorial() {
        return ResponseEntity.ok(backupService.obtenerHistorial());
    }

    @PostMapping("/ejecutar")
    public ResponseEntity<HistorialBackupDTO> ejecutarManual() {
        return ResponseEntity.ok(backupService.ejecutarBackupManual());
    }
}
