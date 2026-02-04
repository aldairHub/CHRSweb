package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.Entity.Institucion;

public interface InstitucionRepository extends JpaRepository<Institucion, Long> {
}
