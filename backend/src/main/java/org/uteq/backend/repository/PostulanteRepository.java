package org.uteq.backend.repository;

import org.uteq.backend.entity.Postulante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PostulanteRepository extends JpaRepository<Postulante, Long> {
    Optional<Postulante> findByPrepostulacion_IdPrepostulacion(Long idPrepostulacion);
}