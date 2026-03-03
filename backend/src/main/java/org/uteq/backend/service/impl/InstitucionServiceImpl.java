package org.uteq.backend.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.entity.Institucion;
import org.uteq.backend.repository.InstitucionRepository;
import org.uteq.backend.service.AesCipherService;
import org.uteq.backend.service.InstitucionService;
import org.uteq.backend.dto.InstitucionRequestDTO;
import org.uteq.backend.dto.InstitucionResponseDTO;
import org.uteq.backend.service.SupabaseStorageService;

@Service
@Transactional
public class InstitucionServiceImpl implements InstitucionService {

    private final InstitucionRepository institucionRepository;
    private final AesCipherService aesCipherService;
    private final SupabaseStorageService storageService;

    // Constructor actualizado
    public InstitucionServiceImpl(InstitucionRepository r, AesCipherService aes, SupabaseStorageService sto) {
        this.institucionRepository = r;
        this.aesCipherService = aes;
        this.storageService = sto;
    }

    // obtener la institución activa
    @Override
    public InstitucionResponseDTO obtenerActiva() {
        return institucionRepository.findByActivoTrue()
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("No hay configuración institucional"));
    }

    @Override
    public InstitucionResponseDTO actualizar(Long id, InstitucionRequestDTO dto) {
        Institucion inst = institucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        inst.setNombre(dto.getNombreInstitucion());
        inst.setDireccion(dto.getDireccion());
        inst.setCorreo(dto.getCorreo());
        inst.setTelefono(dto.getTelefono());
        inst.setAppName(dto.getAppName());
        inst.setEmailSmtp(dto.getEmailSmtp());
        inst.setEmailHost(dto.getEmailHost() != null ? dto.getEmailHost() : "smtp.gmail.com");
        inst.setEmailPort(dto.getEmailPort() != null ? dto.getEmailPort() : 587);
        inst.setEmailSsl(dto.getEmailSsl() != null ? dto.getEmailSsl() : false);
        // Solo actualizar password si se envió uno nuevo
        if (dto.getGmailPassword() != null && !dto.getGmailPassword().isBlank()) {
            inst.setEmailPassword(aesCipherService.cifrar(dto.getGmailPassword()));
        }

        return mapToResponse(institucionRepository.save(inst));
    }

    private InstitucionResponseDTO mapToResponse(Institucion i) {
        InstitucionResponseDTO dto = new InstitucionResponseDTO();
        dto.setIdInstitucion(i.getIdInstitucion());
        dto.setNombreInstitucion(i.getNombre());
        dto.setDireccion(i.getDireccion());
        dto.setCorreo(i.getCorreo());
        dto.setTelefono(i.getTelefono());
        dto.setLogoUrl(i.getLogoUrl());
        dto.setAppName(i.getAppName());
        dto.setEmailSmtp(i.getEmailSmtp());
        dto.setEmailHost(i.getEmailHost());
        dto.setEmailPort(i.getEmailPort());
        dto.setEmailSsl(i.getEmailSsl());
        dto.setTienePasswordConfigurado(i.getEmailPassword() != null);
        return dto;
    }

    @Override
    public String uploadLogo(Long idInstitucion, MultipartFile file) {
        Institucion inst = institucionRepository.findById(idInstitucion)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Solo se permiten archivos de imagen");
        }

        String tag = "inst_" + idInstitucion + "_" + System.currentTimeMillis();

        try {
            String logoUrl = storageService.subirArchivo(file, "logos", tag);
            inst.setLogoUrl(logoUrl);
            institucionRepository.save(inst);
            return logoUrl;
        } catch (Exception e) {
            throw new RuntimeException("Error al subir logo: " + e.getMessage());
        }
    }


    @Override
    public InstitucionResponseDTO crear(InstitucionRequestDTO dto) {

        Institucion institucion = new Institucion();
        institucion.setNombre(dto.getNombreInstitucion());
        institucion.setDireccion(dto.getDireccion());
        institucion.setTelefono(dto.getTelefono());


        return mapToResponse(institucionRepository.save(institucion));
    }

    @Override
    public List<InstitucionResponseDTO> listar() {
        return institucionRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public InstitucionResponseDTO obtenerPorId(Long id) {
        Institucion institucion = institucionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Institucion no encontrada"));
        return mapToResponse(institucion);
    }

//    @Override
//    public InstitucionResponseDTO actualizar(Long id, InstitucionRequestDTO dto) {
//
//        Institucion institucion = institucionRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Institucion no encontrada"));
//
//        institucion.setNombre(dto.getNombreInstitucion());
//        institucion.setDireccion(dto.getDireccion());
//        institucion.setTelefono(dto.getTelefono());
//
//
//        return mapToResponse(institucionRepository.save(institucion));
//    }

    @Override
    public void eliminar(Long id) {
        institucionRepository.deleteById(id);
    }

}
