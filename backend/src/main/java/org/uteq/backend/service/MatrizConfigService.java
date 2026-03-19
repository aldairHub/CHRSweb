package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.MatrizConfigDTO;
import org.uteq.backend.entity.MatrizAccionAfirmativa;
import org.uteq.backend.entity.MatrizItem;
import org.uteq.backend.entity.MatrizSeccion;
import org.uteq.backend.repository.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MatrizConfigService {

    private final MatrizSeccionRepository seccionRepo;
    private final MatrizItemRepository itemRepo;
    private final MatrizAccionAfirmativaRepository accionRepo;
    private final PostgresProcedureRepository procedureRepo;

    // ── Obtener estructura completa ───────────────────────────
    @Transactional(readOnly = true)
    public MatrizConfigDTO obtenerEstructura() {
        List<MatrizSeccion> secciones = seccionRepo.findByActivoTrueOrderByOrdenAsc();

        List<MatrizConfigDTO.SeccionDTO> seccionesDTO = secciones.stream()
                .map(s -> MatrizConfigDTO.SeccionDTO.builder()
                        .idSeccion(s.getIdSeccion())
                        .codigo(s.getCodigo())
                        .titulo(s.getTitulo())
                        .descripcion(s.getDescripcion())
                        .puntajeMaximo(s.getPuntajeMaximo())
                        .orden(s.getOrden())
                        .tipo(s.getTipo())
                        .bloqueado(s.getBloqueado())
                        .items(itemRepo.findBySeccion_IdSeccionAndActivoTrueOrderByOrdenAsc(s.getIdSeccion())
                                .stream()
                                .map(i -> MatrizConfigDTO.ItemDTO.builder()
                                        .idItem(i.getIdItem())
                                        .codigo(i.getCodigo())
                                        .label(i.getLabel())
                                        .puntajeMaximo(i.getPuntajeMaximo())
                                        .puntosPor(i.getPuntosPor())
                                        .orden(i.getOrden())
                                        .bloqueado(i.getBloqueado())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        List<MatrizConfigDTO.AccionAfirmativaDTO> acciones = accionRepo.findByActivoTrueOrderByOrdenAsc()
                .stream()
                .map(a -> MatrizConfigDTO.AccionAfirmativaDTO.builder()
                        .idAccion(a.getIdAccion())
                        .codigo(a.getCodigo())
                        .label(a.getLabel())
                        .puntos(a.getPuntos())
                        .build())
                .collect(Collectors.toList());

        return MatrizConfigDTO.builder()
                .secciones(seccionesDTO)
                .accionesAfirmativas(acciones)
                .build();
    }

    // ── Guardar sección ───────────────────────────────────────
    public void guardarSeccion(Map<String, Object> body) {
        Long idSeccion = body.get("idSeccion") != null ? ((Number) body.get("idSeccion")).longValue() : null;
        String codigo = (String) body.get("codigo");
        String titulo = (String) body.get("titulo");
        String descripcion = (String) body.get("descripcion");
        Double puntajeMax = ((Number) body.get("puntajeMaximo")).doubleValue();
        Integer orden = ((Number) body.get("orden")).intValue();
        String tipo = (String) body.getOrDefault("tipo", "meritos");

        procedureRepo.ejecutarProcedure("CALL sp_guardar_seccion(?,?,?,?,?,?,?)",
                idSeccion, codigo, titulo, descripcion, puntajeMax, orden, tipo);
    }

    // ── Eliminar sección ──────────────────────────────────────
    public void eliminarSeccion(Long idSeccion) {
        MatrizSeccion seccion = seccionRepo.findById(idSeccion)
                .orElseThrow(() -> new RuntimeException("Sección no encontrada"));
        if (Boolean.TRUE.equals(seccion.getBloqueado())) {
            throw new RuntimeException("Esta sección no puede eliminarse");
        }
        procedureRepo.ejecutarProcedure("CALL sp_eliminar_seccion(?)", idSeccion);
    }

    // ── Guardar ítem ──────────────────────────────────────────
    public void guardarItem(Map<String, Object> body) {
        Long idItem = body.get("idItem") != null ? ((Number) body.get("idItem")).longValue() : null;
        Long idSeccion = ((Number) body.get("idSeccion")).longValue();
        String codigo = (String) body.get("codigo");
        String label = (String) body.get("label");
        Double puntajeMax = ((Number) body.get("puntajeMaximo")).doubleValue();
        String puntosPor = (String) body.getOrDefault("puntosPor", null);
        Integer orden = ((Number) body.get("orden")).intValue();

        procedureRepo.ejecutarProcedure("CALL sp_guardar_item_matriz(?,?,?,?,?,?,?)",
                idItem, idSeccion, codigo, label, puntajeMax, puntosPor, orden);
    }

    // ── Eliminar ítem ─────────────────────────────────────────
    public void eliminarItem(Long idItem) {
        MatrizItem item = itemRepo.findById(idItem)
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado"));
        if (Boolean.TRUE.equals(item.getBloqueado())) {
            throw new RuntimeException("Este ítem no puede eliminarse");
        }
        procedureRepo.ejecutarProcedure("CALL sp_eliminar_item_matriz(?)", idItem);
    }
}