package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.PrepostulacionSolicitud;
import org.uteq.backend.entity.PrepostulacionSolicitudId;

import java.util.List;

@Repository
public interface PrepostulacionSolicitudRepository extends JpaRepository<PrepostulacionSolicitud, PrepostulacionSolicitudId> {

    // EmbeddedId → acceso con "id" + nombre del campo: id.idPrepostulacion → findByIdIdPrepostulacion
    List<PrepostulacionSolicitud> findByIdIdPrepostulacion(Long idPrepostulacion);

    List<PrepostulacionSolicitud> findByIdIdSolicitud(Long idSolicitud);
}