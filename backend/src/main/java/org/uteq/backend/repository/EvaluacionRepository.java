package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Evaluacion;

import java.util.List;
import java.util.Optional;

@Repository
public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    List<Evaluacion> findByReunion_IdReunion(Long idReunion);

    Optional<Evaluacion> findByReunion_IdReunionAndEvaluador_IdUsuario(Long idReunion, Long idUsuario);

    boolean existsByReunion_IdReunionAndEvaluador_IdUsuario(Long idReunion, Long idUsuario);

    @Query("SELECT COUNT(e) FROM Evaluacion e WHERE e.confirmada = true")
    Long countConfirmadas();
}
