package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.AudAccion;
import org.uteq.backend.service.AccionAuditoriaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/auditoria/acciones")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AccionAuditoriaController {

    private final AccionAuditoriaService service;

    @GetMapping
    public ResponseEntity<Page<AudAccion>> listar(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String usuarioApp,
            @RequestParam(required = false) String usuarioBd,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());
        return ResponseEntity.ok(
                service.buscar(usuarioApp, usuarioBd, accion, entidad, desde, hasta, pageable)
        );
    }
}