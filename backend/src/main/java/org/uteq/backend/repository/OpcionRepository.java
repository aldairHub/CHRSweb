package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Opcion;
import java.util.List;

@Repository
public interface OpcionRepository extends JpaRepository<Opcion, Integer> {
    List<Opcion> findByModulo_NombreOrderByOrdenAsc(String nombreModulo);
    boolean existsByModulo_IdModuloAndRuta(Integer idModulo, String ruta);
}
