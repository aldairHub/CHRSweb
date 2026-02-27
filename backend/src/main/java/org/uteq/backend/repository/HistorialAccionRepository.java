package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.HistorialAccion;

import java.util.List;

@Repository
public interface HistorialAccionRepository extends JpaRepository<HistorialAccion, Long> {
    List<HistorialAccion> findByProceso_IdProcesoOrderByFechaDesc(Long idProceso);
}
