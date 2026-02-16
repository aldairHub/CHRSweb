package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.Institucion;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {
}
