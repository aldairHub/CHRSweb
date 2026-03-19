package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.Query;
import org.uteq.backend.entity.Postulante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostulanteRepository extends JpaRepository<Postulante, Long> {
    Optional<Postulante> findByPrepostulacion_IdPrepostulacion(Long idPrepostulacion);
    @Query("SELECT p FROM Postulante p LEFT JOIN FETCH p.usuario")
    List<Postulante> findAllConUsuario();
    Optional<Postulante> findByUsuario_Correo(String correo);
    Optional<Postulante> findByUsuario_UsuarioApp(String usuarioApp);
}