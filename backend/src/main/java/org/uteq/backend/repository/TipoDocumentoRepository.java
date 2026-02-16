package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.TipoDocumento;

import java.util.Optional;

@Repository
public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Long> {
    Optional<TipoDocumento> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}