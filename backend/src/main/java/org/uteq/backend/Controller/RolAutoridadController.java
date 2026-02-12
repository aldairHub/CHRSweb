package org.uteq.backend.Controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import org.uteq.backend.Repository.IRolAutoridadRepository;
import org.uteq.backend.Entity.RolAutoridad;
import org.uteq.backend.Service.RolAutoridadService;
import org.uteq.backend.dto.RolAutoridadDTO;
import org.uteq.backend.dto.RolUsuarioResponseDTO;
import org.uteq.backend.dto.RolesUsuarioPorRolAutoridadRequestDTO;

@RestController
@RequestMapping("/api/roles-autoridad")
@CrossOrigin(origins = "*")
public class RolAutoridadController {

    private final IRolAutoridadRepository repo;
    private final RolAutoridadService rolAutoridadService;

    public RolAutoridadController(IRolAutoridadRepository repo, RolAutoridadService rolAutoridadService) {
        this.repo = repo;
        this.rolAutoridadService = rolAutoridadService;
    }

    @PostMapping("/roles-usuario")
    public List<RolUsuarioResponseDTO> rolesUsuario(@RequestBody RolesUsuarioPorRolAutoridadRequestDTO req) {
        return rolAutoridadService.rolesUsuarioPorRolesAutoridad(req.getIdsRolAutoridad());
    }

    @GetMapping
    public List<RolAutoridadDTO> listar() {
        return repo.findAll().stream().map(r -> {
            RolAutoridadDTO dto = new RolAutoridadDTO();
            dto.setIdRolAutoridad(r.getIdRolAutoridad());
            dto.setNombre(r.getNombre());
            return dto;
        }).collect(Collectors.toList());
    }
}
