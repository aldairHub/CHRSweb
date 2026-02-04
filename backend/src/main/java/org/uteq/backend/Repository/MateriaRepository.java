package org.uteq.backend.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.Entity.Materia;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

    List<Materia> findByCarreraIdCarrera(Long idCarrera);
}
