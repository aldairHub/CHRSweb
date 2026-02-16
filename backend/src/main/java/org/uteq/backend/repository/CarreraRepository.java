package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.Carrera;

public interface CarreraRepository extends JpaRepository<Carrera, Long> {
}
