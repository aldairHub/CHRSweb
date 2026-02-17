package org.uteq.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.entity.SolicitudDocente;

import java.util.List;

@Repository
public interface SolicitudDocenteRepository extends JpaRepository<SolicitudDocente, Long> {

    @Procedure(procedureName = "insertar_solicitud_docente")
    Long insertarSolicitudDocente(
            @Param("p_id_autoridad") Long idAutoridad,
            @Param("p_id_carrera") Long idCarrera,
            @Param("p_id_materia") Long idMateria,
            @Param("p_id_area") Long idArea,
            @Param("p_justificacion") String justificacion,
            @Param("p_cantidad_docentes") Long cantidadDocentes,
            @Param("p_nivel_academico") String nivelAcademico,
            @Param("p_exp_prof_min") Long expProfesionalMin,
            @Param("p_exp_doc_min") Long expDocenteMin,
            @Param("p_observaciones") String observaciones
    );

    List<SolicitudDocente> findByEstadoSolicitud(String estado);

    List<SolicitudDocente> findByAutoridadIdAutoridad(Long idAutoridad);

    List<SolicitudDocente> findByCarreraIdCarrera(Long idCarrera);

    @Query("SELECT s FROM SolicitudDocente s WHERE s.carrera.facultad.idFacultad = :idFacultad")
    List<SolicitudDocente> findByFacultadId(@Param("idFacultad") Long idFacultad);

    List<SolicitudDocente> findByAutoridadIdAutoridadAndEstadoSolicitud(Long idAutoridad, String estado);

    List<SolicitudDocente> findByCarreraIdCarreraAndEstadoSolicitud(Long idCarrera, String estado);

    @Query("SELECT s FROM SolicitudDocente s WHERE s.carrera.facultad.idFacultad = :idFacultad AND s.estadoSolicitud = :estado")
    List<SolicitudDocente> findByFacultadIdAndEstado(@Param("idFacultad") Long idFacultad, @Param("estado") String estado);
}
