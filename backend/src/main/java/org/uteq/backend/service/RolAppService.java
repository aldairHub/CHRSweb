package org.uteq.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.RolAppConRolesBdDTO;
import org.uteq.backend.dto.RolAppSaveDTO;
import org.uteq.backend.entity.RolApp;
import org.uteq.backend.entity.RolAppBd;
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

    public RolAppService(RolAppRepository rolAppRepository,
                         RolAppBdRepository rolAppBdRepository) {
        this.rolAppRepository = rolAppRepository;
        this.rolAppBdRepository = rolAppBdRepository;
    }

    // ─── Lecturas ──────────────────────────────────────────────

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

    /** Devuelve lista de roles BD disponibles en pg_roles (prefijo role_*, NOLOGIN). */
    public List<String> listarRolesBdDisponibles() {
        return rolAppBdRepository.findRolesBdDisponibles();
    }

    // ─── Escritura ─────────────────────────────────────────────

    public RolAppConRolesBdDTO crear(RolAppSaveDTO dto) {
        if (rolAppRepository.existsByNombre(dto.getNombre())) {
            throw new RuntimeException("Ya existe un rol con ese nombre: " + dto.getNombre());
        }

        RolApp rol = new RolApp();
        rol.setNombre(dto.getNombre());
        rol.setDescripcion(dto.getDescripcion());
        rol.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        RolApp guardado = rolAppRepository.save(rol);

        sincronizarRolesBd(guardado, dto.getRolesBd());
        return toConRolesBd(guardado);
    }

    public RolAppConRolesBdDTO actualizar(Integer id, RolAppSaveDTO dto) {
        RolApp rol = rolAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RolApp no encontrado: " + id));

        if (dto.getNombre() != null) rol.setNombre(dto.getNombre());
        if (dto.getDescripcion() != null) rol.setDescripcion(dto.getDescripcion());
        if (dto.getActivo() != null) rol.setActivo(dto.getActivo());

        RolApp guardado = rolAppRepository.save(rol);
        sincronizarRolesBd(guardado, dto.getRolesBd());
        return toConRolesBd(guardado);
    }

    public void cambiarEstado(Integer id, Boolean activo) {
        RolApp rol = rolAppRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RolApp no encontrado: " + id));
        rol.setActivo(activo);
        rolAppRepository.save(rol);
    }

    // ─── Helpers ───────────────────────────────────────────────

    /**
     * Reemplaza los mapeos de roles_bd para un rol_app dado.
     * Si rolesBd es null, no modifica los mapeos existentes.
     */
    private void sincronizarRolesBd(RolApp rol, List<String> rolesBd) {
        if (rolesBd == null) return;

        rolAppBdRepository.deleteByRolAppId(rol.getIdRolApp());

        for (String nombreRolBd : rolesBd) {
            if (nombreRolBd != null && !nombreRolBd.isBlank()) {
                RolAppBd mapeo = new RolAppBd();
                mapeo.setRolApp(rol);
                mapeo.setNombreRolBd(nombreRolBd.trim().toLowerCase());
                rolAppBdRepository.save(mapeo);
            }
        }
    }

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