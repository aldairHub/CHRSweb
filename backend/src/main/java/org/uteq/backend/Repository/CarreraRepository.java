package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.Entity.Carrera;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {
}
