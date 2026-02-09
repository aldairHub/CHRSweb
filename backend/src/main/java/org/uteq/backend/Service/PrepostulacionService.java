package org.uteq.backend.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.Entity.Prepostulacion;
import org.uteq.backend.Repository.PrepostulacionRepository;
import org.uteq.backend.dto.PrepostulacionResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrepostulacionService {

    private final PrepostulacionRepository prepostulacionRepository;
    private final SupabaseStorageService supabaseService;

    @Transactional
    public PrepostulacionResponseDTO procesarPrepostulacion(
            String correo,
            String cedula,
            String nombres,
            String apellidos,
            MultipartFile archivoCedula,
            MultipartFile archivoFoto,
            MultipartFile archivoPrerrequisitos
    ) {
        System.out.println("🔄 Procesando prepostulación para identificación: " + cedula);

        // Validar que no exista duplicado
        if (prepostulacionRepository.existsByIdentificacion(cedula)) {
            throw new RuntimeException("Ya existe una solicitud con esta identificación");
        }

        // Crear entidad
        Prepostulacion prepostulacion = new Prepostulacion();
        prepostulacion.setCorreo(correo);
        prepostulacion.setIdentificacion(cedula);
        prepostulacion.setNombres(nombres);
        prepostulacion.setApellidos(apellidos);
        prepostulacion.setEstadoRevision("PENDIENTE");
        prepostulacion.setFechaEnvio(LocalDateTime.now());

        // ✅ SUBIR ARCHIVOS A SUPABASE
        try {
            System.out.println("📤 Subiendo cédula a Supabase...");
            String urlCedula = supabaseService.subirArchivo(
                    archivoCedula,
                    "cedulas",
                    cedula
            );
            prepostulacion.setUrlCedula(urlCedula);

            System.out.println("📤 Subiendo foto a Supabase...");
            String urlFoto = supabaseService.subirArchivo(
                    archivoFoto,
                    "fotos",
                    cedula
            );
            prepostulacion.setUrlFoto(urlFoto);

            System.out.println("📤 Subiendo prerrequisitos a Supabase...");
            String urlPrerrequisitos = supabaseService.subirArchivo(
                    archivoPrerrequisitos,
                    "prerrequisitos",
                    cedula
            );
            prepostulacion.setUrlPrerrequisitos(urlPrerrequisitos);

            System.out.println("✅ Todos los archivos subidos exitosamente");

        } catch (Exception e) {
            System.err.println("❌ Error al subir archivos: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al subir archivos: " + e.getMessage());
        }

        // Guardar en BD
        Prepostulacion guardado = prepostulacionRepository.save(prepostulacion);
        System.out.println("💾 Prepostulación guardada en BD con ID: " + guardado.getIdPrepostulacion());

        // ✅ RETURN CORREGIDO - Asegúrate que los tipos coincidan
        return new PrepostulacionResponseDTO(
                "Solicitud registrada exitosamente",
                guardado.getCorreo(),
                guardado.getIdPrepostulacion(),  // Long
                true,                             // Boolean
                guardado.getFechaEnvio()         // LocalDateTime
        );
    }

    /**
     * Obtener una prepostulación por ID
     */
    public Prepostulacion obtenerPorId(Long id) {
        return prepostulacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prepostulación no encontrada"));
    }

    /**
     * Listar todas las prepostulaciones (más recientes primero)
     */
    public List<Prepostulacion> listarTodas() {
        return prepostulacionRepository.findAllByOrderByFechaEnvioDesc();
    }

    /**
     * Listar por estado
     */
    public List<Prepostulacion> listarPorEstado(String estado) {
        return prepostulacionRepository.findByEstadoRevision(estado);
    }

    /**
     * Actualizar estado de revisión
     */
    @Transactional
    public void actualizarEstado(Long id, String nuevoEstado, String observaciones, Long idRevisor) {
        Prepostulacion prepostulacion = obtenerPorId(id);
        prepostulacion.setEstadoRevision(nuevoEstado);
        prepostulacion.setObservacionesRevision(observaciones);
        prepostulacion.setFechaRevision(LocalDateTime.now());
        prepostulacion.setIdRevisor(idRevisor);
        prepostulacionRepository.save(prepostulacion);
        System.out.println("✅ Estado actualizado a: " + nuevoEstado);
    }
}