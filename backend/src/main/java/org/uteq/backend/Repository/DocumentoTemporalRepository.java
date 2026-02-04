package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.Entity.DocumentoTemporal;

import java.util.List;

@Repository
public interface DocumentoTemporalRepository extends JpaRepository<DocumentoTemporal, Long> {
    List<DocumentoTemporal> findByIdPrepostulacion(Long idPrepostulacion);
    void deleteByIdPrepostulacion(Long idPrepostulacion);
}