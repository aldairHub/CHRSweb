package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.RequisitoPrepostulacion;
import java.util.List;

@Repository
public interface RequisitoPrepostulacionRepository
        extends JpaRepository<RequisitoPrepostulacion, Long> {

    List<RequisitoPrepostulacion> findByIdSolicitudAndActivoTrueOrderByOrdenAscIdRequisitoAsc(Long idSolicitud);
}