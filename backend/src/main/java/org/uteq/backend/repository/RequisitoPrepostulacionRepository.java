package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.RequisitoPrepostulacion;
import java.util.List;

public interface RequisitoPrepostulacionRepository
        extends JpaRepository<RequisitoPrepostulacion, Long> {

    List<RequisitoPrepostulacion> findByIdSolicitudAndActivoTrueOrderByOrdenAscIdRequisitoAsc(Long idSolicitud);
}