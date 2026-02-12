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
import java.util.stream.Collectors;

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

        return new PrepostulacionResponseDTO(
                "Solicitud registrada exitosamente",
                guardado.getCorreo(),
                guardado.getIdPrepostulacion(),
                true,
                guardado.getFechaEnvio()
        );
    }

    /**
     * Obtener una prepostulación por ID
     */
    public Prepostulacion obtenerPorId(Long id) {
        return prepostulacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prepostulación no encontrada con ID: " + id));
    }

    /**
     * Listar todas las prepostulaciones (más recientes primero)
     */
    public List<Prepostulacion> listarTodas() {
        return prepostulacionRepository.findAllByOrderByFechaEnvioDesc();
    }

    /**
     * Listar por estado de revisión
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

        System.out.println("✅ Estado de prepostulación " + id + " actualizado a: " + nuevoEstado);
    }

    /**
     * Buscar prepostulaciones por identificación, nombre o apellido
     */
    public List<Prepostulacion> buscar(String query) {
        List<Prepostulacion> todas = prepostulacionRepository.findAll();

        String queryLower = query.toLowerCase().trim();

        return todas.stream()
                .filter(p ->
                        p.getIdentificacion().toLowerCase().contains(queryLower) ||
                                p.getNombres().toLowerCase().contains(queryLower) ||
                                p.getApellidos().toLowerCase().contains(queryLower) ||
                                p.getCorreo().toLowerCase().contains(queryLower)
                )
                .collect(Collectors.toList());
    }

    /**
     * Eliminar una prepostulación
     * IMPORTANTE: También elimina los archivos de Supabase
     */
    @Transactional
    public void eliminar(Long id) {
        Prepostulacion prepostulacion = obtenerPorId(id);

        // Eliminar archivos de Supabase primero
        try {
            if (prepostulacion.getUrlCedula() != null) {
                supabaseService.eliminarArchivo(prepostulacion.getUrlCedula());
            }
            if (prepostulacion.getUrlFoto() != null) {
                supabaseService.eliminarArchivo(prepostulacion.getUrlFoto());
            }
            if (prepostulacion.getUrlPrerrequisitos() != null) {
                supabaseService.eliminarArchivo(prepostulacion.getUrlPrerrequisitos());
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error al eliminar archivos de Supabase: " + e.getMessage());
            // Continuamos con la eliminación de la BD aunque falle Supabase
        }

        // Eliminar de la base de datos
        prepostulacionRepository.deleteById(id);

        System.out.println("🗑️ Prepostulación " + id + " eliminada correctamente");
    }

    /**
     * Contar prepostulaciones por estado
     */
    public long contarPorEstado(String estado) {
        return prepostulacionRepository.findByEstadoRevision(estado).size();
    }
}