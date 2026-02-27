package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.FaseProceso;

import java.util.List;
import java.util.Optional;

@Repository
public interface FaseProcesoRepository extends JpaRepository<FaseProceso, Long> {
    List<FaseProceso> findByProceso_IdProcesoOrderByFase_OrdenAsc(Long idProceso);
    Optional<FaseProceso> findByProceso_IdProcesoAndFase_IdFase(Long idProceso, Long idFase);
}
