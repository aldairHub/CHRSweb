package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.DetalleEvaluacion;

import java.util.List;

@Repository
public interface DetalleEvaluacionRepository extends JpaRepository<DetalleEvaluacion, Long> {
    List<DetalleEvaluacion> findByEvaluacion_IdEvaluacion(Long idEvaluacion);
}
