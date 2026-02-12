package org.uteq.backend.Service;

import org.springframework.stereotype.Service;
import org.uteq.backend.Repository.IRolAutoridadRepository;
import org.uteq.backend.dto.RolUsuarioResponseDTO;

import java.util.List;

@Service
public class RolAutoridadService {

    private final IRolAutoridadRepository rolAutoridadRepository;

    public RolAutoridadService(IRolAutoridadRepository rolAutoridadRepository) {
        this.rolAutoridadRepository = rolAutoridadRepository;
    }

    public List<RolUsuarioResponseDTO> rolesUsuarioPorRolesAutoridad(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return rolAutoridadRepository.findRolesUsuarioByIdsRolAutoridad(ids);
    }
}