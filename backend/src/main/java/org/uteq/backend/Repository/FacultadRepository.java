package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.Entity.Facultad;

public interface FacultadRepository extends JpaRepository<Facultad, Long> {
}
