package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Reunion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReunionRepository extends JpaRepository<Reunion, Long> {

    List<Reunion> findByEstadoOrderByFechaAscHoraAsc(String estado);

    @Query("SELECT COUNT(r) FROM Reunion r WHERE r.estado = 'programada'")
    Long countProgramadas();

    @Query("SELECT COUNT(r) FROM Reunion r WHERE r.fecha = :fecha AND r.estado IN ('programada', 'en_curso')")
    Long countByFecha(LocalDate fecha);

    Optional<Reunion> findByFaseProceso_IdFaseProceso(Long idFaseProceso);
}
