package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.NivelAcademico;

import java.util.List;

public interface NivelAcademicoRepository extends JpaRepository<NivelAcademico, Long> {
    List<NivelAcademico> findAllByOrderByOrdenAsc();
    List<NivelAcademico> findByEstadoTrueOrderByOrdenAsc();
}