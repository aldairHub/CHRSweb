package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Convocatoria;
import java.util.List;

@Repository
public interface ConvocatoriaRepository extends JpaRepository<Convocatoria, Long> {

    List<Convocatoria> findByEstadoConvocatoriaOrderByFechaPublicacionDesc(String estado);

    List<Convocatoria> findAllByOrderByFechaPublicacionDesc();
}