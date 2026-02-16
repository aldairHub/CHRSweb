package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.Facultad;

public interface FacultadRepository extends JpaRepository<Facultad, Long> {
}
