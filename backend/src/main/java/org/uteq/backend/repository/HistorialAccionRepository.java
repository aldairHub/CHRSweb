package org.uteq.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.HistorialAccion;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistorialAccionRepository extends JpaRepository<HistorialAccion, Long> {

    List<HistorialAccion> findByProceso_IdProcesoOrderByFechaDesc(Long idProceso);

    @Query(value = """
    SELECT * FROM historial_accion h
    WHERE h.usuario ILIKE CONCAT('%', COALESCE(:usuario, ''), '%')
      AND h.fecha >= COALESCE(:desde, h.fecha)
      AND h.fecha <= COALESCE(:hasta, h.fecha)
    ORDER BY h.fecha DESC
""", nativeQuery = true)
    Page<HistorialAccion> buscarFiltrado(
            @Param("usuario") String usuario,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
