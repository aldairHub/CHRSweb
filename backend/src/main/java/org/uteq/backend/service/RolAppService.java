package org.uteq.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.RolAppConRolesBdDTO;
import org.uteq.backend.dto.RolAppSaveDTO;
import org.uteq.backend.entity.RolApp;
import org.uteq.backend.entity.RolAppBd;
import org.uteq.backend.repository.PostgresProcedureRepository;
import org.uteq.backend.repository.RolAppBdRepository;
import org.uteq.backend.repository.RolAppRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de roles_app y su mapeo con roles_bd.
 *
 * WHY: Reemplaza RolAutoridadService. Toda la lógica de seguridad
 * de aplicación ahora gira en torno a roles_app (tabla) y sus
 * mapeos a roles de BD (pg_roles con prefijo role_*).
 */
@Service
@Transactional
public class RolAppService {

    private final RolAppRepository rolAppRepository;
    private final RolAppBdRepository rolAppBdRepository;
    private final PostgresProcedureRepository procedureRepository;

    public RolAppService(RolAppRepository rolAppRepository,
                         RolAppBdRepository rolAppBdRepository,
                         PostgresProcedureRepository procedureRepository) {
        this.rolAppRepository = rolAppRepository;
        this.rolAppBdRepository = rolAppBdRepository;
        this.procedureRepository = procedureRepository;
    }

    // ─── Lecturas —────────────────────────────────

    public List<RolAppConRolesBdDTO> listarConRolesBd() {
        return rolAppRepository.findAll()
                .stream()
                .map(this::toConRolesBd)
                .collect(Collectors.toList());
    }

    public RolAppConRolesBdDTO obtenerPorId(Integer id) {
        RolApp rol = rolAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RolApp no encontrado: " + id));
        return toConRolesBd(rol);
    }

    public List<String> listarRolesBdDisponibles() {
        return rolAppBdRepository.findRolesBdDisponibles();
    }

    // ─── Escritura —con SPs ────────────────────────────

    public RolAppConRolesBdDTO crear(RolAppSaveDTO dto) {
        if (rolAppRepository.existsByNombre(dto.getNombre()))
            throw new RuntimeException("Ya existe un rol con ese nombre: " + dto.getNombre());

        //  SP hace INSERT en roles_app + valida y asigna roles_bd en roles_app_bd
        Integer idCreado = procedureRepository.crearRolApp(
                dto.getNombre(),
                dto.getDescripcion(),
                dto.getRolesBd()
        );

        return obtenerPorId(idCreado);
    }

    public RolAppConRolesBdDTO actualizar(Integer id, RolAppSaveDTO dto) {
        if (!rolAppRepository.existsById(id))
            throw new RuntimeException("RolApp no encontrado: " + id);

        // SP hace UPDATE en roles_app + DELETE/INSERT en roles_app_bd + valida pg_roles
        procedureRepository.actualizarRolApp(
                id,
                dto.getNombre(),
                dto.getDescripcion(),
                dto.getRolesBd()
        );

        // activo no tiene SP — sigue en JPA
        if (dto.getActivo() != null)
            cambiarEstado(id, dto.getActivo());

        return obtenerPorId(id);
    }

    public void cambiarEstado(Integer id, Boolean activo) {
        RolApp rol = rolAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RolApp no encontrado: " + id));
        rol.setActivo(activo);
        rolAppRepository.save(rol);
    }

    // ─── Helpers —────────────

    private RolAppConRolesBdDTO toConRolesBd(RolApp rol) {
        RolAppConRolesBdDTO dto = new RolAppConRolesBdDTO();
        dto.setIdRolApp(rol.getIdRolApp());
        dto.setNombre(rol.getNombre());
        dto.setDescripcion(rol.getDescripcion());
        dto.setActivo(rol.getActivo());
        dto.setFechaCreacion(rol.getFechaCreacion());

        List<String> rolesBd = rolAppBdRepository.findByRolApp_IdRolApp(rol.getIdRolApp())
                .stream()
                .map(RolAppBd::getNombreRolBd)
                .sorted()
                .collect(Collectors.toList());

        dto.setRolesBd(rolesBd);
        return dto;
    }

}