package org.uteq.backend.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PrepostulacionRequestDTO {
    private String correo;
    private String cedula;
    private String nombres;
    private String apellidos;

    // Archivos
    private MultipartFile archivoCedula;
    private MultipartFile archivoFoto;
    private MultipartFile archivoPrerrequisitos;
}