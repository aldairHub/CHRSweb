package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.MatrizAccionAfirmativa;
import java.util.List;

public interface MatrizAccionAfirmativaRepository extends JpaRepository<MatrizAccionAfirmativa, Long> {
    List<MatrizAccionAfirmativa> findByActivoTrueOrderByOrdenAsc();
}