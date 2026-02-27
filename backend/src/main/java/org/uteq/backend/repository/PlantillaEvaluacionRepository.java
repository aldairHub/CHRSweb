package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.PlantillaEvaluacion;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantillaEvaluacionRepository extends JpaRepository<PlantillaEvaluacion, Long> {
    boolean existsByCodigo(String codigo);
    Optional<PlantillaEvaluacion> findByFase_IdFase(Long idFase);
    List<PlantillaEvaluacion> findAllByOrderByUltimaModificacionDesc();
}
