package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.MatrizItem;
import java.util.List;

public interface MatrizItemRepository extends JpaRepository<MatrizItem, Long> {
    List<MatrizItem> findBySeccion_IdSeccionAndActivoTrueOrderByOrdenAsc(Long idSeccion);
}
