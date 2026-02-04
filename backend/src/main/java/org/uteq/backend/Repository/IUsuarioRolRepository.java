package org.uteq.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.uteq.backend.Entity.UsuarioRol;
import org.uteq.backend.Entity.UsuarioRolId;

import java.util.List;

public interface IUsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {
    @Query("""
    select r.nombre
    from UsuarioRol ur
    join ur.rol r
    where ur.usuario.idUsuario = :idUsuario
  """)
    List<String> findRoleNamesByUserId(Long idUsuario);
}