package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.dto.DocumentoReutilizableDTO;
import org.uteq.backend.dto.PrepostulacionResponseDTO;
import org.uteq.backend.entity.PrepostulacionDocumento;
import org.uteq.backend.entity.Postulante;
import org.uteq.backend.entity.PrepostulacionSolicitud;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NuevaPostulacionService {

    private static final Logger log = LoggerFactory.getLogger(NuevaPostulacionService.class);

    private final UsuarioRepository                usuarioRepository;
    private final PostulanteRepository             postulanteRepository;
    private final PrepostulacionSolicitudRepository prepostulacionSolicitudRepository;
    private final PostgresProcedureRepository          proc;
    private final PrepostulacionDocumentoRepository    documentoRepository;
    private final PrepostulacionRepository             prepostulacionRepository;
    private final SupabaseStorageService           supabase;
    private final NotificacionService              notifService;
    private final ProcesoEvaluacionRepository      procesoEvaluacionRepository;

    // ── Documentos anteriores del postulante (cédula, foto y académicos) ─────
    public List<DocumentoReutilizableDTO> getMisDocumentos(String usuarioApp) {
        Postulante postulante = postulanteRepository.findByUsuario_UsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("Postulante no encontrado"));

        // Todas las prepostulaciones del postulante ordenadas por fecha desc (más reciente primero)
        List<org.uteq.backend.entity.Prepostulacion> todasPrepostulaciones =
                prepostulacionRepository.findByIdentificacion(postulante.getIdentificacion())
                        .stream()
                        .sorted(java.util.Comparator.comparing(
                                p -> p.getFechaEnvio() != null ? p.getFechaEnvio() :
                                        java.time.LocalDateTime.MIN,
                                java.util.Comparator.reverseOrder()))
                        .collect(java.util.stream.Collectors.toList());

        List<DocumentoReutilizableDTO> resultado = new java.util.ArrayList<>();

        // Cédula — la más reciente que tenga url_cedula no nula
        todasPrepostulaciones.stream()
                .filter(p -> p.getUrlCedula() != null && !p.getUrlCedula().isBlank())
                .findFirst()
                .ifPresent(p -> resultado.add(new DocumentoReutilizableDTO(
                        null, "CEDULA", "Cédula / Pasaporte",
                        p.getUrlCedula(),
                        p.getFechaEnvio() != null ? p.getFechaEnvio().toString() : null)));

        // Foto — la más reciente que tenga url_foto no nula
        todasPrepostulaciones.stream()
                .filter(p -> p.getUrlFoto() != null && !p.getUrlFoto().isBlank())
                .findFirst()
                .ifPresent(p -> resultado.add(new DocumentoReutilizableDTO(
                        null, "FOTO", "Foto de perfil",
                        p.getUrlFoto(),
                        p.getFechaEnvio() != null ? p.getFechaEnvio().toString() : null)));

        // Documentos académicos libres — de todas las prepostulaciones, deduplicados por URL
        todasPrepostulaciones.stream()
                .flatMap(p -> documentoRepository.findByIdPrepostulacion(p.getIdPrepostulacion()).stream())
                .filter(d -> d.getIdRequisito() == null)
                .filter(d -> d.getUrlDocumento() != null)
                .collect(java.util.stream.Collectors.toMap(
                        PrepostulacionDocumento::getUrlDocumento,
                        d -> d,
                        (a, b) -> a
                ))
                .values().stream()
                .map(d -> new DocumentoReutilizableDTO(
                        d.getIdDocumento(), "ACADEMICO",
                        d.getDescripcion(), d.getUrlDocumento(),
                        d.getFechaSubida() != null ? d.getFechaSubida().toString() : null
                ))
                .sorted(java.util.Comparator.comparing(DocumentoReutilizableDTO::getDescripcion))
                .forEach(resultado::add);

        return resultado;
    }

    @Transactional
    public PrepostulacionResponseDTO postular(
            String usuarioApp,
            Long idSolicitud,
            List<MultipartFile> archivosDocumentos,
            List<String> descripcionesDocumentos,
            List<MultipartFile> archivosRequisitos,
            List<Long> idsRequisitos,
            List<String> nombresRequisitos,
            List<Long> idsDocumentosReutilizados,
            String urlCedulaReutilizada,
            String urlFotoReutilizada,
            MultipartFile archivoCedulaNueva,
            MultipartFile archivoFotoNueva
    ) {
        log.info("Nueva postulación de {} a solicitud {}", usuarioApp, idSolicitud);

        // 1. Obtener usuario y postulante
        Usuario usuario = usuarioRepository.findByUsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + usuarioApp));

        Postulante postulante = postulanteRepository.findByUsuario_UsuarioApp(usuarioApp)
                .orElseThrow(() -> new RuntimeException("No se encontró el postulante para este usuario."));

        // 2. Verificar que no haya postulado ya a esta solicitud
        if (yaPostuloASolicitud(usuarioApp, idSolicitud)) {
            throw new RuntimeException("Ya tienes una postulación activa para esta plaza.");
        }

        // 3. Resolver cédula y foto — reutilizada o nueva subida
        // Validar que no haya conflicto (reutilizar Y subir nueva al mismo tiempo)
        if (urlCedulaReutilizada != null && archivoCedulaNueva != null && !archivoCedulaNueva.isEmpty()) {
            throw new RuntimeException("No puede reutilizar la cédula y subir una nueva al mismo tiempo. Elija una opción.");
        }
        if (urlFotoReutilizada != null && archivoFotoNueva != null && !archivoFotoNueva.isEmpty()) {
            throw new RuntimeException("No puede reutilizar la foto y subir una nueva al mismo tiempo. Elija una opción.");
        }

        String urlCedula = urlCedulaReutilizada;
        String urlFoto   = urlFotoReutilizada;

        if (urlCedula == null) {
            if (archivoCedulaNueva != null && !archivoCedulaNueva.isEmpty()) {
                try {
                    urlCedula = supabase.subirArchivo(archivoCedulaNueva, "cedulas",
                            postulante.getIdentificacion() + "_nueva_" + System.currentTimeMillis());
                } catch (Exception e) {
                    throw new RuntimeException("Error subiendo nueva cédula: " + e.getMessage());
                }
            } else {
                // Fallback: reutilizar de la prepostulacion original
                urlCedula = postulante.getPrepostulacion() != null
                        ? postulante.getPrepostulacion().getUrlCedula() : null;
            }
        }

        if (urlFoto == null) {
            if (archivoFotoNueva != null && !archivoFotoNueva.isEmpty()) {
                try {
                    urlFoto = supabase.subirArchivo(archivoFotoNueva, "fotos",
                            postulante.getIdentificacion() + "_nueva_" + System.currentTimeMillis());
                } catch (Exception e) {
                    throw new RuntimeException("Error subiendo nueva foto: " + e.getMessage());
                }
            } else {
                urlFoto = postulante.getPrepostulacion() != null
                        ? postulante.getPrepostulacion().getUrlFoto() : null;
            }
        }

        Long idPrepostulacion = proc.registrarPrepostulacion(
                postulante.getNombresPostulante(),
                postulante.getApellidosPostulante(),
                postulante.getIdentificacion(),
                postulante.getCorreoPostulante(),
                urlCedula,
                urlFoto,
                idSolicitud
        );
        log.info("Nueva prepostulacion creada con ID: {}", idPrepostulacion);

        // 4. Subir documentos académicos libres (nuevos)
        if (archivosDocumentos != null && !archivosDocumentos.isEmpty()) {
            String tag = postulante.getIdentificacion() + "_nueva_" + System.currentTimeMillis();
            for (int i = 0; i < archivosDocumentos.size(); i++) {
                MultipartFile archivo = archivosDocumentos.get(i);
                if (archivo == null || archivo.isEmpty()) continue;
                String desc = (descripcionesDocumentos != null && i < descripcionesDocumentos.size())
                        ? descripcionesDocumentos.get(i) : "Documento " + (i + 1);
                try {
                    String url = supabase.subirArchivo(archivo, "documentos_academicos", tag + "_" + i);
                    proc.agregarDocumentoPrepostulacion(idPrepostulacion, desc, url);
                } catch (Exception e) {
                    throw new RuntimeException("Error subiendo documento académico: " + e.getMessage());
                }
            }
        }

        // 5. Reutilizar documentos seleccionados de prepostulaciones anteriores
        if (idsDocumentosReutilizados != null && !idsDocumentosReutilizados.isEmpty()) {
            for (Long idDoc : idsDocumentosReutilizados) {
                documentoRepository.findById(idDoc).ifPresent(doc -> {
                    try {
                        proc.agregarDocumentoPrepostulacion(
                                idPrepostulacion, doc.getDescripcion(), doc.getUrlDocumento());
                        log.info("Documento {} reutilizado para prepostulacion {}", idDoc, idPrepostulacion);
                    } catch (Exception e) {
                        log.warn("No se pudo reutilizar documento {}: {}", idDoc, e.getMessage());
                    }
                });
            }
        }

        // 6. Subir documentos de requisitos obligatorios
        if (archivosRequisitos != null && idsRequisitos != null) {
            for (int i = 0; i < archivosRequisitos.size(); i++) {
                MultipartFile archivo = archivosRequisitos.get(i);
                Long idReq = (i < idsRequisitos.size()) ? idsRequisitos.get(i) : null;
                if (archivo == null || archivo.isEmpty()) continue;
                String desc = (nombresRequisitos != null && i < nombresRequisitos.size())
                        ? nombresRequisitos.get(i) : archivo.getOriginalFilename();
                try {
                    String url = supabase.subirArchivo(archivo, "documentos_requisitos",
                            postulante.getIdentificacion() + "_req_" + idReq + "_" + System.currentTimeMillis());
                    proc.agregarDocumentoPrepostulacion(idPrepostulacion, desc, url, idReq);
                } catch (Exception e) {
                    throw new RuntimeException("Error subiendo documento de requisito: " + e.getMessage());
                }
            }
        }

        // 7. Notificar al revisor — la prepostulacion queda PENDIENTE de revisión
        notifService.notifRevisorNuevaPrepostulacion(idPrepostulacion,
                postulante.getNombresPostulante() + " " + postulante.getApellidosPostulante());
        log.info("Prepostulacion {} enviada a revisión para postulante {}", idPrepostulacion, postulante.getIdPostulante());

        return new PrepostulacionResponseDTO(
                "Postulación enviada exitosamente.", usuario.getCorreo(),
                idPrepostulacion, true, LocalDateTime.now()
        );
    }

    public boolean yaPostuloASolicitud(String usuarioApp, Long idSolicitud) {
        return postulanteRepository.findByUsuario_UsuarioApp(usuarioApp)
                .map(postulante -> {
                    // Bloquear si ya tiene un proceso de evaluación activo para esta solicitud
                    // (en_proceso, pendiente, completado — todo menos rechazado)
                    boolean tieneProcesoActivo = procesoEvaluacionRepository
                            .existsByPostulante_IdPostulanteAndSolicitudDocente_IdSolicitud(
                                    postulante.getIdPostulante(), idSolicitud);
                    if (tieneProcesoActivo) return true;

                    // También verificar prepostulaciones pendientes de revisión vinculadas a esta solicitud
                    // (aún no han sido aprobadas, por lo que no tienen proceso_evaluacion todavía)
                    return prepostulacionRepository
                            .findAll()
                            .stream()
                            .filter(pp -> pp.getCorreo().equals(postulante.getCorreoPostulante()))
                            .filter(pp -> !"RECHAZADO".equalsIgnoreCase(pp.getEstadoRevision()))
                            .anyMatch(pp -> prepostulacionSolicitudRepository
                                    .findByIdIdPrepostulacion(pp.getIdPrepostulacion())
                                    .stream()
                                    .anyMatch(ps -> ps.getId().getIdSolicitud().equals(idSolicitud)));
                })
                .orElse(false);
    }
}