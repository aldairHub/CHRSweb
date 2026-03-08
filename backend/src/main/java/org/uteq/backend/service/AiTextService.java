package org.uteq.backend.service;

import java.util.List;

public interface AiTextService {

    /** Contexto de una solicitud: nombre de materia + justificación */
    record SolicitudContexto(String nombreMateria, String justificacion) {}

    String generateConvocatoriaDescripcion(List<SolicitudContexto> contextos);
}