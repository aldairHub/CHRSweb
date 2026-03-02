package org.uteq.backend.repository;

import org.uteq.backend.entity.ConvocatoriaSolicitud;
import org.uteq.backend.entity.ConvocatoriaSolicitud.ConvocatoriaSolicitudId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConvocatoriaSolicitudRepository
        extends JpaRepository<ConvocatoriaSolicitud, ConvocatoriaSolicitudId> {

    // Con @EmbeddedId el path es: id.idConvocatoria
    @Query("SELECT c FROM ConvocatoriaSolicitud c WHERE c.id.idConvocatoria = :idConvocatoria")
    List<ConvocatoriaSolicitud> findByIdConvocatoria(@Param("idConvocatoria") Long idConvocatoria);

    @Query("SELECT COUNT(c) FROM ConvocatoriaSolicitud c WHERE c.id.idConvocatoria = :idConvocatoria")
    long countByIdConvocatoria(@Param("idConvocatoria") Long idConvocatoria);

    @Modifying
    @Query("DELETE FROM ConvocatoriaSolicitud c WHERE c.id.idConvocatoria = :idConvocatoria")
    void deleteByIdConvocatoria(@Param("idConvocatoria") Long idConvocatoria);
}