package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Prepostulacion;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrepostulacionRepository extends JpaRepository<Prepostulacion, Long> {

    List<Prepostulacion> findAllByOrderByFechaEnvioDesc();

    List<Prepostulacion> findByEstadoRevision(String estadoRevision);

    // Ya no se usa para bloquear duplicados, pero puede servir para buscar todas las de una cédula
    List<Prepostulacion> findByIdentificacion(String identificacion);

    // Para verificar estado y repostular: trae la más reciente de esa cédula
    Optional<Prepostulacion> findTopByIdentificacionOrderByFechaEnvioDesc(String identificacion);

    // Sigue siendo útil para contar
    boolean existsByIdentificacion(String identificacion);
}