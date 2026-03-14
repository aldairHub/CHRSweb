package org.uteq.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.AudCambioDTO;
import org.uteq.backend.entity.AudCambio;
import org.uteq.backend.repository.AudCambioRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


@RestController
@RequestMapping("/api/admin/auditoria/cambios")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AudCambioController {

    private final AudCambioRepository repo;

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    public ResponseEntity<Page<AudCambioDTO>> listar(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String tabla,
            @RequestParam(required = false) String operacion,
            @RequestParam(required = false) String campo,
            @RequestParam(required = false) String usuarioApp,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("fecha").descending());

        String desdeStr = desde != null ? desde.atStartOfDay().format(TS_FMT)             : null;
        String hastaStr = hasta != null ? hasta.plusDays(1).atStartOfDay().format(TS_FMT) : null;

        Page<AudCambioDTO> resultado = repo.buscar(
                blank(tabla),
                blank(operacion),
                blank(campo),
                blank(usuarioApp),
                desdeStr,
                hastaStr,
                pageable
        ).map(this::toDTO);

        return ResponseEntity.ok(resultado);
    }

    // ── Mapeo entidad → DTO ───────────────────────────────────────────────

    private AudCambioDTO toDTO(AudCambio c) {
        AudCambioDTO dto = new AudCambioDTO();
        dto.setIdAudCambio(c.getIdAudCambio());
        dto.setTabla(c.getTabla());
        dto.setIdRegistro(c.getIdRegistro());
        dto.setOperacion(c.getOperacion());
        dto.setCampo(c.getCampo());
        dto.setValorAntes(c.getValorAntes());
        dto.setValorDespues(c.getValorDespues());
        dto.setUsuarioBd(c.getUsuarioBd());
        dto.setUsuarioApp(c.getUsuarioApp());
        dto.setIpCliente(c.getIpCliente());
        dto.setFecha(c.getFecha() != null ? c.getFecha().toString() : null);
        return dto;
    }

    private String blank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}