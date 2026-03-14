package org.uteq.backend.service;

import org.uteq.backend.dto.ConvocatoriaDTO.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.SolicitudDocenteResponseDTO;
import org.uteq.backend.entity.SolicitudDocente;
import org.uteq.backend.repository.ConvocatoriaSolicitudRepository;
import org.uteq.backend.repository.SolicitudDocenteRepository;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConvocatoriaAdminService {

    private final JdbcTemplate                      jdbcTemplate;
    private final ConvocatoriaSolicitudRepository    convSolicitudRepo;
    private final SolicitudDocenteRepository         solicitudRepo;

    // ── SOLICITUDES DISPONIBLES ───────────────────────────────────────────

    // Excluye las que ya están asociadas a convocatorias 'abierta' o 'en_proceso'
    @Transactional(readOnly = true)
    public List<SolicitudDocenteResponseDTO> getSolicitudesDisponiblesParaConvocatoria() {
        List<Long> idsOcupados = convSolicitudRepo.findIdsSolicitudesEnConvocatoriasActivas();

        // Si no hay ninguna ocupada, traemos todas las aprobadas con JOIN FETCH
        // pasando [-1L] para que el NOT IN no falle con lista vacía en JPQL
        List<Long> param = idsOcupados.isEmpty() ? List.of(-1L) : idsOcupados;

        return solicitudRepo.findDisponiblesParaConvocatoria(param)
                .stream()
                .map(this::mapSolicitudToDTO)
                .collect(Collectors.toList());
    }

    private SolicitudDocenteResponseDTO mapSolicitudToDTO(SolicitudDocente s) {
        return SolicitudDocenteResponseDTO.builder()
                .idSolicitud(s.getIdSolicitud())
                .idCarrera(s.getCarrera() != null ? s.getCarrera().getIdCarrera() : null)
                .nombreCarrera(s.getCarrera() != null ? s.getCarrera().getNombreCarrera() : null)
                .idFacultad(s.getCarrera() != null && s.getCarrera().getFacultad() != null
                        ? s.getCarrera().getFacultad().getIdFacultad() : null)
                .nombreFacultad(s.getCarrera() != null && s.getCarrera().getFacultad() != null
                        ? s.getCarrera().getFacultad().getNombreFacultad() : null)
                .idMateria(s.getMateria() != null ? s.getMateria().getIdMateria() : null)
                .nombreMateria(s.getMateria() != null ? s.getMateria().getNombreMateria() : null)
                .idArea(s.getArea() != null ? s.getArea().getIdArea() : null)
                .nombreArea(s.getArea() != null ? s.getArea().getNombreArea() : null)
                .estadoSolicitud(s.getEstadoSolicitud())
                .cantidadDocentes(s.getCantidadDocentes())
                .nivelAcademico(s.getNivelAcademico())
                .experienciaProfesionalMin(s.getExperienciaProfesionalMin())
                .experienciaDocenteMin(s.getExperienciaDocenteMin())
                .fechaSolicitud(s.getFechaSolicitud())
                // justificacion NO se mapea: no debe ser visible al postulante
                .build();
    }

    // ── CREAR ─────────────────────────────────────────────────────────────

    @Transactional
    public MensajeResponse crear(CrearRequest req, String usuarioApp, String usuarioBd, String ip) {
        try {
            Long[] idsArray = req.getIdsSolicitudes() == null
                    ? new Long[0]
                    : req.getIdsSolicitudes().toArray(new Long[0]);

            Array sqlArray = jdbcTemplate.execute(
                    (Connection con) -> con.createArrayOf("bigint", idsArray)
            );

            Map<String, Object> result = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("sp_crear_convocatoria")
                    .execute(new MapSqlParameterSource()
                            .addValue("p_titulo",            req.getTitulo())
                            .addValue("p_descripcion",       req.getDescripcion())
                            .addValue("p_fecha_publicacion", Date.valueOf(req.getFechaPublicacion()))
                            .addValue("p_fecha_inicio",      Date.valueOf(req.getFechaInicio()))
                            .addValue("p_fecha_fin",         Date.valueOf(req.getFechaFin()))
                            .addValue("p_ids_solicitudes",   sqlArray)
                    );

            String mensaje = (String) result.get("p_mensaje");
            Long   idNuevo = result.get("p_id_convocatoria") != null
                    ? ((Number) result.get("p_id_convocatoria")).longValue()
                    : null;

            boolean exito = idNuevo != null;


            return MensajeResponse.builder()
                    .exito(exito)
                    .mensaje(mensaje)
                    .data(exito ? Map.of("idConvocatoria", idNuevo) : null)
                    .build();

        } catch (Exception e) {
            return MensajeResponse.builder().exito(false)
                    .mensaje("Error al crear: " + e.getMessage()).build();
        }
    }

    /** Sobrecarga sin auditoría — para llamadas internas del sistema */
    @Transactional
    public MensajeResponse crear(CrearRequest req) {
        return crear(req, "sistema", "sistema", null);
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────

    @Transactional
    public MensajeResponse actualizar(Long id, ActualizarRequest req, String usuarioApp, String usuarioBd, String ip) {
        try {
            Map<String, Object> result = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("sp_actualizar_convocatoria")
                    .execute(new MapSqlParameterSource()
                            .addValue("p_id_convocatoria",   id)
                            .addValue("p_titulo",            req.getTitulo())
                            .addValue("p_descripcion",       req.getDescripcion())
                            .addValue("p_fecha_publicacion", Date.valueOf(req.getFechaPublicacion()))
                            .addValue("p_fecha_inicio",      Date.valueOf(req.getFechaInicio()))
                            .addValue("p_fecha_fin",         Date.valueOf(req.getFechaFin()))
                    );

            int    filas   = ((Number) result.get("p_filas")).intValue();
            String mensaje = (String) result.get("p_mensaje");



            return MensajeResponse.builder().exito(filas > 0).mensaje(mensaje).build();

        } catch (Exception e) {
            return MensajeResponse.builder().exito(false)
                    .mensaje("Error al actualizar: " + e.getMessage()).build();
        }
    }

    /** Sobrecarga sin auditoría — para llamadas internas del sistema */
    @Transactional
    public MensajeResponse actualizar(Long id, ActualizarRequest req) {
        return actualizar(id, req, "sistema", "sistema", null);
    }

    // ── CAMBIAR ESTADO ────────────────────────────────────────────────────

    @Transactional
    public MensajeResponse cambiarEstado(Long id, String nuevoEstado, String usuarioApp, String usuarioBd, String ip) {
        try {
            Map<String, Object> result = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("sp_cambiar_estado_convocatoria")
                    .execute(new MapSqlParameterSource()
                            .addValue("p_id_convocatoria", id)
                            .addValue("p_nuevo_estado",    nuevoEstado)
                    );

            String mensaje = (String) result.get("p_mensaje");
            boolean exito  = !mensaje.startsWith("Error");



            return MensajeResponse.builder().exito(exito).mensaje(mensaje).build();

        } catch (Exception e) {
            return MensajeResponse.builder().exito(false)
                    .mensaje("Error al cambiar estado: " + e.getMessage()).build();
        }
    }

    /** Sobrecarga sin auditoría — para llamadas internas del sistema */
    @Transactional
    public MensajeResponse cambiarEstado(Long id, String nuevoEstado) {
        return cambiarEstado(id, nuevoEstado, "sistema", "sistema", null);
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────

    @Transactional
    public MensajeResponse eliminar(Long id) {
        try {
            Map<String, Object> result = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("sp_eliminar_convocatoria")
                    .execute(new MapSqlParameterSource()
                            .addValue("p_id_convocatoria", id)
                    );

            String mensaje = (String) result.get("p_mensaje");
            return MensajeResponse.builder()
                    .exito(!mensaje.startsWith("Error"))
                    .mensaje(mensaje).build();

        } catch (Exception e) {
            return MensajeResponse.builder().exito(false)
                    .mensaje("Error al eliminar: " + e.getMessage()).build();
        }
    }

    // ── LISTAR ────────────────────────────────────────────────────────────

    public List<ListaResponse> listar(String estado, String titulo) {
        return jdbcTemplate.query(
                "SELECT * FROM fn_listar_convocatorias(?::varchar, ?::varchar)",
                new Object[]{ estado, titulo },
                (rs, rowNum) -> ListaResponse.builder()
                        .idConvocatoria(rs.getLong("id_convocatoria"))
                        .titulo(rs.getString("titulo"))
                        .descripcion(rs.getString("descripcion"))
                        .fechaPublicacion(rs.getDate("fecha_publicacion").toLocalDate())
                        .fechaInicio(rs.getDate("fecha_inicio").toLocalDate())
                        .fechaFin(rs.getDate("fecha_fin").toLocalDate())
                        .estadoConvocatoria(rs.getString("estado_convocatoria"))
                        .totalSolicitudes(rs.getLong("total_solicitudes"))
                        .build()
        );
    }

    // ── DETALLE ───────────────────────────────────────────────────────────

    public DetalleResponse detalle(Long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM fn_detalle_convocatoria(?)", id
        );
        if (rows.isEmpty()) return null;

        Map<String, Object> first = rows.get(0);
        List<SolicitudResumen> solicitudes = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            if (row.get("id_solicitud") != null) {
                solicitudes.add(SolicitudResumen.builder()
                        .idSolicitud(((Number) row.get("id_solicitud")).longValue())
                        .nombreMateria((String) row.get("nombre_materia"))
                        .nombreCarrera((String) row.get("nombre_carrera"))
                        .nombreFacultad((String) row.get("nombre_facultad"))
                        .cantidadDocentes(((Number) row.get("cantidad_docentes")).longValue())
                        .nivelAcademico((String) row.get("nivel_academico"))
                        .estadoSolicitud((String) row.get("estado_solicitud"))
                        .build());
            }
        }

        return DetalleResponse.builder()
                .idConvocatoria(((Number) first.get("id_convocatoria")).longValue())
                .titulo((String) first.get("titulo"))
                .descripcion((String) first.get("descripcion"))
                .fechaPublicacion(((Date) first.get("fecha_publicacion")).toLocalDate())
                .fechaInicio(((Date) first.get("fecha_inicio")).toLocalDate())
                .fechaFin(((Date) first.get("fecha_fin")).toLocalDate())
                .estadoConvocatoria((String) first.get("estado_convocatoria"))
                .solicitudes(solicitudes)
                .build();
    }
}