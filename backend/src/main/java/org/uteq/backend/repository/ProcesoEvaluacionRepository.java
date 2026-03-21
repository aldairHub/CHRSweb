package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.ProcesoEvaluacion;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcesoEvaluacionRepository extends JpaRepository<ProcesoEvaluacion, Long> {

    List<ProcesoEvaluacion> findAllByOrderByFechaInicioDesc();

    List<ProcesoEvaluacion> findByEstadoGeneral(String estadoGeneral);

    Optional<ProcesoEvaluacion> findByCodigo(String codigo);

    List<ProcesoEvaluacion> findBySolicitudDocente_IdSolicitudOrderByFechaInicioDesc(Long idSolicitud);

    // Solo postulantes habilitados para entrevista: puntaje >= 50 o habilitado manualmente
    @Query("SELECT p FROM ProcesoEvaluacion p WHERE p.solicitudDocente.idSolicitud = :idSolicitud " +
            "AND (p.puntajeMatriz >= 50 OR p.habilitadoEntrevista = true) " +
            "ORDER BY p.fechaInicio DESC")
    List<ProcesoEvaluacion> findHabilitadosParaEntrevistaBySolicitud(Long idSolicitud);

    @Query("SELECT p FROM ProcesoEvaluacion p WHERE " +
            "(p.puntajeMatriz >= 50 OR p.habilitadoEntrevista = true) " +
            "ORDER BY p.fechaInicio DESC")
    List<ProcesoEvaluacion> findHabilitadosParaEntrevista();

    boolean existsByPostulante_IdPostulanteAndSolicitudDocente_IdSolicitud(Long idPostulante, Long idSolicitud);

    @Query("SELECT COUNT(p) FROM ProcesoEvaluacion p WHERE p.estadoGeneral NOT IN ('completado', 'rechazado')")
    Long countActivos();
}
