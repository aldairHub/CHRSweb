package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.entity.PrepostulacionDocumento;
import java.util.List;

public interface PrepostulacionDocumentoRepository
        extends JpaRepository<PrepostulacionDocumento, Long> {

    List<PrepostulacionDocumento> findByIdPrepostulacion(Long idPrepostulacion);

    void deleteByIdPrepostulacion(Long idPrepostulacion);
}
