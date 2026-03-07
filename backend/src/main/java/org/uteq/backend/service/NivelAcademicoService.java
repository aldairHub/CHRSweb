package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.NivelAcademicoRequestDTO;
import org.uteq.backend.dto.NivelAcademicoResponseDTO;
import org.uteq.backend.entity.NivelAcademico;
import org.uteq.backend.repository.NivelAcademicoRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NivelAcademicoService {

    private final NivelAcademicoRepository repo;

    // ── Listar todos (admin) ─────────────────────────────────
    @Transactional(readOnly = true)
    public List<NivelAcademicoResponseDTO> listar() {
        return repo.findAllByOrderByOrdenAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Listar solo activos (para los selects del formulario) ─
    @Transactional(readOnly = true)
    public List<NivelAcademicoResponseDTO> listarActivos() {
        return repo.findByEstadoTrueOrderByOrdenAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ── Crear ────────────────────────────────────────────────
    public NivelAcademicoResponseDTO crear(NivelAcademicoRequestDTO dto) {
        String nombre = validarNombre(dto.getNombre());

        NivelAcademico n = new NivelAcademico();
        n.setNombre(nombre);
        n.setOrden(dto.getOrden() != null ? dto.getOrden() : 0);
        n.setEstado(dto.getEstado() != null ? dto.getEstado() : true);

        return toDTO(repo.save(n));
    }

    // ── Actualizar ───────────────────────────────────────────
    public NivelAcademicoResponseDTO actualizar(Long id, NivelAcademicoRequestDTO dto) {
        NivelAcademico n = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Nivel académico no encontrado"));

        String nombre = validarNombre(dto.getNombre());
        n.setNombre(nombre);
        if (dto.getOrden()  != null) n.setOrden(dto.getOrden());
        if (dto.getEstado() != null) n.setEstado(dto.getEstado());

        return toDTO(repo.save(n));
    }

    // ── Eliminar ─────────────────────────────────────────────
    public void eliminar(Long id) {
        repo.findById(id).orElseThrow(() -> new RuntimeException("Nivel académico no encontrado"));
        repo.deleteById(id);
    }

    // ── Helpers ──────────────────────────────────────────────
    private String validarNombre(String raw) {
        String nombre = raw == null ? "" : raw.trim();
        if (nombre.isEmpty()) throw new RuntimeException("El nombre del nivel académico es obligatorio");
        return nombre;
    }

    private NivelAcademicoResponseDTO toDTO(NivelAcademico n) {
        NivelAcademicoResponseDTO dto = new NivelAcademicoResponseDTO();
        dto.setIdNivel(n.getIdNivel());
        dto.setNombre(n.getNombre());
        dto.setOrden(n.getOrden());
        dto.setEstado(n.isEstado());
        return dto;
    }
}