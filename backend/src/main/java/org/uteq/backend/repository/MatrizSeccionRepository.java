package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.MatrizSeccion;
import java.util.List;

public interface MatrizSeccionRepository extends JpaRepository<MatrizSeccion, Long> {
    List<MatrizSeccion> findByActivoTrueOrderByOrdenAsc();
}

