package org.uteq.backend.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.Repository.IRolAutoridadRepository;
import org.uteq.backend.Repository.IRolUsuarioRepository;
import org.uteq.backend.Entity.RolAutoridad;
import org.uteq.backend.Entity.RolUsuario;
import org.uteq.backend.dto.RolAutoridadSaveDTO;
import org.uteq.backend.dto.RolUsuarioResponseDTO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RolAutoridadService {

    private final IRolAutoridadRepository rolAutoridadRepository;
    private final IRolUsuarioRepository rolUsuarioRepository;

    public RolAutoridadService(IRolAutoridadRepository rolAutoridadRepository,
                               IRolUsuarioRepository rolUsuarioRepository) {
        this.rolAutoridadRepository = rolAutoridadRepository;
        this.rolUsuarioRepository = rolUsuarioRepository;
    }

    // ✅ LO QUE YA TENÍAS (NO TOCAR)
    public List<RolUsuarioResponseDTO> rolesUsuarioPorRolesAutoridad(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return rolAutoridadRepository.findRolesUsuarioByIdsRolAutoridad(ids);
    }

    // =========================
    // ✅ NUEVO: CREAR
    // =========================
    @Transactional
    public RolAutoridad crearRolAutoridad(RolAutoridadSaveDTO dto) {
        if (dto == null) throw new RuntimeException("Body vacío");

        RolAutoridad rol = new RolAutoridad();
        rol.setNombre(normalizarNombre(dto.getNombre()));

        Set<RolUsuario> roles = cargarRolesUsuario(dto.getRolesUsuarioIds());
        rol.setRolesUsuario(roles);

        return rolAutoridadRepository.save(rol);
    }

    // =========================
    // ✅ NUEVO: ACTUALIZAR
    // =========================
    @Transactional
    public RolAutoridad actualizarRolAutoridad(Long idRolAutoridad, RolAutoridadSaveDTO dto) {
        if (dto == null) throw new RuntimeException("Body vacío");

        RolAutoridad rol = rolAutoridadRepository.findById(idRolAutoridad)
                .orElseThrow(() -> new RuntimeException("RolAutoridad no encontrado: " + idRolAutoridad));

        rol.setNombre(normalizarNombre(dto.getNombre()));

        Set<RolUsuario> roles = cargarRolesUsuario(dto.getRolesUsuarioIds());

        // ✅ reemplazar relaciones sin duplicar
        rol.getRolesUsuario().clear();
        rol.getRolesUsuario().addAll(roles);

        return rolAutoridadRepository.save(rol);
    }

    // =========================
    // HELPERS
    // =========================
    private String normalizarNombre(String nombre) {
        String n = (nombre == null) ? "" : nombre.trim();
        if (n.length() < 3) throw new RuntimeException("Nombre inválido (mínimo 3 caracteres)");
        return n;
    }

    private Set<RolUsuario> cargarRolesUsuario(List<Long> rolesUsuarioIds) {
        if (rolesUsuarioIds == null || rolesUsuarioIds.isEmpty()) return Set.of();

        // distinct manteniendo orden
        Set<Long> idsUnicos = new LinkedHashSet<>();
        for (Long id : rolesUsuarioIds) {
            if (id != null) idsUnicos.add(id);
        }

        List<RolUsuario> encontrados = rolUsuarioRepository.findAllById(idsUnicos);

        // opcional: validar que existan todos
        if (encontrados.size() != idsUnicos.size()) {
            throw new RuntimeException("Uno o más roles_usuario no existen");
        }

        return new LinkedHashSet<>(encontrados);
    }
}
