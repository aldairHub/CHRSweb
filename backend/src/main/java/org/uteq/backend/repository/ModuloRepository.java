package org.uteq.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.Modulo;
import java.util.Optional;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Integer> {
    Optional<Modulo> findByNombre(String nombre);
    boolean existsByNombre(String nombre);
}
