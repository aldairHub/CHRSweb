package org.uteq.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para crear una AutoridadAcademica vinculando un usuario existente
 * (que NO esté ya registrado como autoridad ni como postulante).
 */
@Data
public class AutoridadDesdeUsuarioRequestDTO {

    private Long idUsuario;          // usuario existente a vincular
    private String nombres;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;
    private Long idInstitucion;
    private Long idFacultad;
    private List<String> rolesApp;   // roles a asignar al usuario
}