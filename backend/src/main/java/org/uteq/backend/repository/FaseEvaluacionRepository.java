package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.FaseEvaluacion;

import java.util.List;

@Repository
public interface FaseEvaluacionRepository extends JpaRepository<FaseEvaluacion, Long> {
    List<FaseEvaluacion> findAllByOrderByOrdenAsc();
    List<FaseEvaluacion> findByEstadoTrueOrderByOrdenAsc();
}
