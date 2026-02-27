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

    boolean existsByPostulante_IdPostulanteAndSolicitudDocente_IdSolicitud(Long idPostulante, Long idSolicitud);

    @Query("SELECT COUNT(p) FROM ProcesoEvaluacion p WHERE p.estadoGeneral NOT IN ('completado', 'rechazado')")
    Long countActivos();
}
