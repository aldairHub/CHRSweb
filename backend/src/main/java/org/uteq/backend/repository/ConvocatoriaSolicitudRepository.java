package org.uteq.backend.repository;

import org.uteq.backend.entity.ConvocatoriaSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConvocatoriaSolicitudRepository extends JpaRepository<ConvocatoriaSolicitud, Long> {
    List<ConvocatoriaSolicitud> findByIdConvocatoria(Long idConvocatoria);
}