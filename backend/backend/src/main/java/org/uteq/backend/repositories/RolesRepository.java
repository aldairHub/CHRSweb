package org.uteq.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entities.Roles;

@Repository
public interface RolesRepository extends JpaRepository<Roles,Long> {
}
