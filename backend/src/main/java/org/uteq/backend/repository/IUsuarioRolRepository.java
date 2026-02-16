//package org.uteq.backend.repository;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;
//import org.uteq.backend.entity.UsuarioRol;
//import org.uteq.backend.entity.UsuarioRolId;
//
//import java.util.List;
//
//@Repository
//public interface IUsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {
//    @Query("""
//    select r.nombre
//    from UsuarioRol ur
//    join ur.rol r
//    where ur.usuario.idUsuario = :idUsuario
//  """)
//    List<String> findRoleNamesByUserId(Long idUsuario);
//}