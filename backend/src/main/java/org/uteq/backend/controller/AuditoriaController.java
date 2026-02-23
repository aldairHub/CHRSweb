package org.uteq.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.AudLoginApp;
import org.uteq.backend.repository.AudLoginAppJpaRepository;
import org.uteq.backend.service.AuditoriaService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/auditoria")
@CrossOrigin(origins = "*")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;
    private final AudLoginAppJpaRepository auditoriaRepository;

    public AuditoriaController(AuditoriaService auditoriaService,
                               AudLoginAppJpaRepository auditoriaRepository) {
        this.auditoriaService    = auditoriaService;
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping("/login")
    public ResponseEntity<Page<AudLoginApp>> listar(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String usuarioApp,
            @RequestParam(required = false)    String resultado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        return ResponseEntity.ok(
                auditoriaService.buscar(usuarioApp, resultado, desde, hasta, pageable)
        );
    }

    @GetMapping("/login/stats")
    public ResponseEntity<List<Object[]>> stats() {
        return ResponseEntity.ok(auditoriaRepository.statsUltimos7Dias());
    }
}