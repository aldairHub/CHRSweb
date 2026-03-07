package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.ConvocatoriaTipoDocumento;

import java.util.List;

@Repository
public interface ConvocatoriaTipoDocumentoRepository
        extends JpaRepository<ConvocatoriaTipoDocumento, Long> {

    List<ConvocatoriaTipoDocumento> findByConvocatoriaIdConvocatoria(Long idConvocatoria);

    @Modifying
    @Query("DELETE FROM ConvocatoriaTipoDocumento c WHERE c.convocatoria.idConvocatoria = :idConvocatoria")
    void deleteByConvocatoriaIdConvocatoria(@Param("idConvocatoria") Long idConvocatoria);
}