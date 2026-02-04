package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.Entity.AreaConocimiento;

@Repository
public interface AreaConocimientoRepository
        extends JpaRepository<AreaConocimiento, Long> {

    // Por ahora no necesitas métodos personalizados
}
