package org.uteq.backend.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

import org.uteq.backend.Repository.IRolAutoridadRepository;
import org.uteq.backend.Entity.RolAutoridad;
import org.uteq.backend.Service.RolAutoridadService;
import org.uteq.backend.dto.RolAutoridadDTO;
import org.uteq.backend.dto.RolAutoridadSaveDTO;
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


    @GetMapping("/con-roles-usuario")
    public List<RolAutoridadDTO> listarConRolesUsuario() {

        // 1) Traes el catálogo base
        List<RolAutoridadDTO> roles = repo.findAll().stream().map(r -> {
            RolAutoridadDTO dto = new RolAutoridadDTO();
            dto.setIdRolAutoridad(r.getIdRolAutoridad());
            dto.setNombre(r.getNombre());
            return dto;
        }).collect(Collectors.toList());

        // 2) Para cada rol, se cargas sus roles_usuario asociados usando TU service existente
        for (RolAutoridadDTO ra : roles) {
            List<RolUsuarioResponseDTO> rus =
                    rolAutoridadService.rolesUsuarioPorRolesAutoridad(
                            List.of(ra.getIdRolAutoridad())
                    );
            ra.setRolesUsuario(rus);
        }

        return roles;
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody RolAutoridadSaveDTO dto) {
        return ResponseEntity.ok(rolAutoridadService.crearRolAutoridad(dto));
    }

    @PutMapping("/{idRolAutoridad}")
    public ResponseEntity<?> actualizar(@PathVariable Long idRolAutoridad,
                                        @RequestBody RolAutoridadSaveDTO dto) {
        return ResponseEntity.ok(rolAutoridadService.actualizarRolAutoridad(idRolAutoridad, dto));
    }

    @GetMapping
    public List<RolAutoridadDTO> listar() {
        return repo.findAllConRolesUsuario().stream().map(r -> {

            RolAutoridadDTO dto = new RolAutoridadDTO();
            dto.setIdRolAutoridad(r.getIdRolAutoridad());
            dto.setNombre(r.getNombre());

            var roles = r.getRolesUsuario().stream().map(ru -> {
                RolUsuarioResponseDTO rDto = new RolUsuarioResponseDTO();
                rDto.setIdRolUsuario(ru.getIdRolUsuario());
                rDto.setNombre(ru.getNombre());
                return rDto;
            }).toList();

            dto.setRolesUsuario(roles);

            return dto;

        }).collect(Collectors.toList());
    }
}
