package org.uteq.backend.repository;

import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.ConvocatoriaSolicitud;
import org.uteq.backend.entity.ConvocatoriaSolicitud.ConvocatoriaSolicitudId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ConvocatoriaSolicitudRepository
        extends JpaRepository<ConvocatoriaSolicitud, ConvocatoriaSolicitudId> {

    @Query("SELECT c FROM ConvocatoriaSolicitud c WHERE c.id.idConvocatoria = :idConvocatoria")
    List<ConvocatoriaSolicitud> findByIdConvocatoria(@Param("idConvocatoria") Long idConvocatoria);

    @Query("SELECT c FROM ConvocatoriaSolicitud c WHERE c.id.idSolicitud = :idSolicitud")
    List<ConvocatoriaSolicitud> findByIdSolicitud(@Param("idSolicitud") Long idSolicitud);

    @Query("SELECT COUNT(c) FROM ConvocatoriaSolicitud c WHERE c.id.idConvocatoria = :idConvocatoria")
    long countByIdConvocatoria(@Param("idConvocatoria") Long idConvocatoria);

    @Modifying
    @Query("DELETE FROM ConvocatoriaSolicitud c WHERE c.id.idConvocatoria = :idConvocatoria")
    void deleteByIdConvocatoria(@Param("idConvocatoria") Long idConvocatoria);

    // ── PASO 6: IDs de solicitudes que ya están en convocatorias activas ──
    // Usa SQL nativo porque ConvocatoriaSolicitud no tiene @ManyToOne a Convocatoria,
    // por lo que JPQL no puede hacer JOIN libre entre las dos entidades.
    @Query(value = """
        SELECT cs.id_solicitud
        FROM convocatoria_solicitud cs
        JOIN convocatoria c ON c.id_convocatoria = cs.id_convocatoria
        WHERE c.estado_convocatoria IN ('abierta', 'en_proceso')
    """, nativeQuery = true)
    List<Long> findIdsSolicitudesEnConvocatoriasActivas();
}