package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.CriterioEvaluacionRequestDTO;
import org.uteq.backend.dto.CriterioEvaluacionResponseDTO;
import org.uteq.backend.entity.CriterioEvaluacion;
import org.uteq.backend.entity.PlantillaEvaluacion;
import org.uteq.backend.repository.CriterioEvaluacionRepository;
import org.uteq.backend.repository.PlantillaEvaluacionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CriterioEvaluacionService {

    private final CriterioEvaluacionRepository criterioRepository;
    private final PlantillaEvaluacionRepository plantillaRepository;

    // ─── Listar por plantilla ──────────────────────────────────
    @Transactional(readOnly = true)
    public List<CriterioEvaluacionResponseDTO> listarPorPlantilla(Long idPlantilla) {
        return criterioRepository.findByPlantilla_IdPlantilla(idPlantilla)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ─── Obtener por ID ────────────────────────────────────────
    @Transactional(readOnly = true)
    public CriterioEvaluacionResponseDTO obtenerPorId(Long id) {
        return toDTO(findOrThrow(id));
    }

    // ─── Crear ─────────────────────────────────────────────────
    public CriterioEvaluacionResponseDTO crear(CriterioEvaluacionRequestDTO dto) {
        PlantillaEvaluacion plantilla = plantillaRepository.findById(dto.getIdPlantilla())
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada con id: " + dto.getIdPlantilla()));

        // Validar que la suma de pesos no supere 100
        Integer pesoActual = criterioRepository.sumPesoByPlantilla(dto.getIdPlantilla());
        int totalNuevo = (pesoActual != null ? pesoActual : 0) + dto.getPeso();
        if (totalNuevo > 100)
            throw new RuntimeException("El peso total de los criterios superaría 100% (actual: "
                    + pesoActual + "%, agregando: " + dto.getPeso() + "%)");

        CriterioEvaluacion criterio = new CriterioEvaluacion();
        criterio.setNombre(dto.getNombre());
        criterio.setDescripcion(dto.getDescripcion());
        criterio.setPeso(dto.getPeso());
        criterio.setEscala(dto.getEscala() != null ? dto.getEscala() : "1-5");
        criterio.setRubrica(dto.getRubrica());
        criterio.setPlantilla(plantilla);

        // Actualizar fecha modificación de la plantilla
        plantilla.setUltimaModificacion(LocalDateTime.now());
        plantillaRepository.save(plantilla);

        return toDTO(criterioRepository.save(criterio));
    }

    // ─── Actualizar ────────────────────────────────────────────
    public CriterioEvaluacionResponseDTO actualizar(Long id, CriterioEvaluacionRequestDTO dto) {
        CriterioEvaluacion criterio = findOrThrow(id);

        // Si cambia el peso, verificar que no supere 100
        if (dto.getPeso() != null && !dto.getPeso().equals(criterio.getPeso())) {
            Integer pesoActual = criterioRepository.sumPesoByPlantilla(criterio.getPlantilla().getIdPlantilla());
            int pesoSinEste   = (pesoActual != null ? pesoActual : 0) - criterio.getPeso();
            if (pesoSinEste + dto.getPeso() > 100)
                throw new RuntimeException("El nuevo peso superaría 100% total.");
            criterio.setPeso(dto.getPeso());
        }

        if (dto.getNombre()      != null) criterio.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) criterio.setDescripcion(dto.getDescripcion());
        if (dto.getEscala()      != null) criterio.setEscala(dto.getEscala());
        if (dto.getRubrica()     != null) criterio.setRubrica(dto.getRubrica());

        // Actualizar fecha modificación de la plantilla
        PlantillaEvaluacion plantilla = criterio.getPlantilla();
        plantilla.setUltimaModificacion(LocalDateTime.now());
        plantillaRepository.save(plantilla);

        return toDTO(criterioRepository.save(criterio));
    }

    // ─── Eliminar ──────────────────────────────────────────────
    public void eliminar(Long id) {
        CriterioEvaluacion criterio = findOrThrow(id);
        PlantillaEvaluacion plantilla = criterio.getPlantilla();
        criterioRepository.deleteById(id);
        plantilla.setUltimaModificacion(LocalDateTime.now());
        plantillaRepository.save(plantilla);
    }

    // ─── Mapper ────────────────────────────────────────────────
    private CriterioEvaluacionResponseDTO toDTO(CriterioEvaluacion c) {
        return new CriterioEvaluacionResponseDTO(
                c.getIdCriterio(),
                c.getNombre(),
                c.getDescripcion(),
                c.getPeso(),
                c.getEscala(),
                c.getRubrica(),
                c.getPlantilla().getIdPlantilla()
        );
    }

    private CriterioEvaluacion findOrThrow(Long id) {
        return criterioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Criterio no encontrado con id: " + id));
    }
}
