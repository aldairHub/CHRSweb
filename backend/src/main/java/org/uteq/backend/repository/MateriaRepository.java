package org.uteq.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.Materia;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    List<Materia> findByCarreraIdCarrera(Long idCarrera);
}
