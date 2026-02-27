package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.CriterioEvaluacion;

import java.util.List;

@Repository
public interface CriterioEvaluacionRepository extends JpaRepository<CriterioEvaluacion, Long> {
    List<CriterioEvaluacion> findByPlantilla_IdPlantilla(Long idPlantilla);

    @Query("SELECT SUM(c.peso) FROM CriterioEvaluacion c WHERE c.plantilla.idPlantilla = :idPlantilla")
    Integer sumPesoByPlantilla(Long idPlantilla);
}
