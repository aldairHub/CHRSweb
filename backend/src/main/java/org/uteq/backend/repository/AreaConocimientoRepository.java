package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.AreaConocimiento;

@Repository
public interface AreaConocimientoRepository
        extends JpaRepository<AreaConocimiento, Long> {
}
