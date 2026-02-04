package org.uteq.backend.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.uteq.backend.Entity.DocumentoTemporal;
import org.uteq.backend.Entity.Prepostulacion;
import org.uteq.backend.Entity.TipoDocumento;
import org.uteq.backend.Entity.Usuario;
import org.uteq.backend.Repository.DocumentoTemporalRepository;
import org.uteq.backend.Repository.PrepostulacionRepository;
import org.uteq.backend.Repository.TipoDocumentoRepository;
import org.uteq.backend.Repository.UsuarioRepository;
import org.uteq.backend.dto.PrepostulacionResponseDTO;
import org.uteq.backend.util.CredencialesGenerator;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PrepostulacionService {

    private final PrepostulacionRepository prepostulacionRepository;
    private final DocumentoTemporalRepository documentoTemporalRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

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
        try {
            // 1. VALIDACIONES PREVIAS
            validarDatos(correo, cedula, nombres, apellidos);
            validarArchivos(archivoCedula, archivoFoto, archivoPrerrequisitos);

            // Verificar si ya existe prepostulación
            if (prepostulacionRepository.existsByIdentificacion(cedula)) {
                throw new RuntimeException("Ya existe una prepostulación con esta cédula");
            }
            if (prepostulacionRepository.existsByCorreo(correo)) {
                throw new RuntimeException("Ya existe una prepostulación con este correo");
            }

            // 2. CREAR PREPOSTULACIÓN
            Prepostulacion prepostulacion = new Prepostulacion();
            prepostulacion.setNombres(nombres);
            prepostulacion.setApellidos(apellidos);
            prepostulacion.setIdentificacion(cedula);
            prepostulacion.setCorreo(correo);
            prepostulacion.setEstadoRevision("pendiente");

            Prepostulacion prepostulacionGuardada = prepostulacionRepository.save(prepostulacion);

            // 3. GUARDAR ARCHIVOS
            guardarDocumento(prepostulacionGuardada.getIdPrepostulacion(), archivoCedula, "CEDULA");
            guardarDocumento(prepostulacionGuardada.getIdPrepostulacion(), archivoFoto, "FOTO_CEDULA");
            guardarDocumento(prepostulacionGuardada.getIdPrepostulacion(), archivoPrerrequisitos, "PRERREQUISITOS");

            // 4. GENERAR CREDENCIALES Y CREAR USUARIO
            String usuarioApp = CredencialesGenerator.generarUsuario(cedula);
            String claveAppPlain = CredencialesGenerator.generarClaveApp();
            String claveBd = CredencialesGenerator.generarClaveBd();
            String usuarioBd = "usuario" + cedula.substring(cedula.length() - 6);

            // Verificar si el usuario ya existe (por si acaso)
            int contador = 1;
            String usuarioAppOriginal = usuarioApp;
            while (usuarioRepository.existsByUsuarioApp(usuarioApp)) {
                usuarioApp = usuarioAppOriginal + contador;
                contador++;
            }

            // Crear usuario
            Usuario usuario = new Usuario();
            usuario.setUsuarioBd(usuarioBd);
            usuario.setClaveBd(claveBd);
            usuario.setUsuarioApp(usuarioApp);
            usuario.setClaveApp(passwordEncoder.encode(claveAppPlain));
            usuario.setCorreo(correo);
            usuario.setActivo(true);

            usuarioRepository.save(usuario);

            // 5. ENVIAR EMAIL CON CREDENCIALES
            try {
                emailService.enviarCredenciales(correo, usuarioApp, claveAppPlain);
            } catch (Exception e) {
                System.err.println("Error al enviar email: " + e.getMessage());
                // No fallar el registro si el email falla
            }

            // 6. RETORNAR RESPUESTA
            return new PrepostulacionResponseDTO(
                    "Prepostulación registrada exitosamente. Se han enviado las credenciales a tu correo.",
                    correo,
                    usuarioApp,
                    true,
                    prepostulacionGuardada.getIdPrepostulacion()
            );

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar prepostulación: " + e.getMessage());
        }
    }

    private void validarDatos(String correo, String cedula, String nombres, String apellidos) {
        if (correo == null || correo.isBlank()) {
            throw new RuntimeException("El correo es obligatorio");
        }
        if (cedula == null || cedula.isBlank()) {
            throw new RuntimeException("La cédula es obligatoria");
        }
        if (nombres == null || nombres.isBlank()) {
            throw new RuntimeException("Los nombres son obligatorios");
        }
        if (apellidos == null || apellidos.isBlank()) {
            throw new RuntimeException("Los apellidos son obligatorios");
        }
        if (cedula.length() != 10) {
            throw new RuntimeException("La cédula debe tener 10 dígitos");
        }
    }

    private void validarArchivos(MultipartFile cedula, MultipartFile foto, MultipartFile prerrequisitos) {
        if (cedula == null || cedula.isEmpty()) {
            throw new RuntimeException("Debe subir el archivo de cédula");
        }
        if (foto == null || foto.isEmpty()) {
            throw new RuntimeException("Debe subir la foto selfie con cédula");
        }
        if (prerrequisitos == null || prerrequisitos.isEmpty()) {
            throw new RuntimeException("Debe subir los pre-requisitos");
        }

        // Validar tipos de archivo
        if (!cedula.getContentType().equals("application/pdf")) {
            throw new RuntimeException("La cédula debe ser un archivo PDF");
        }
        if (!prerrequisitos.getContentType().equals("application/pdf")) {
            throw new RuntimeException("Los pre-requisitos deben ser un archivo PDF");
        }
        if (!foto.getContentType().startsWith("image/")) {
            throw new RuntimeException("La foto debe ser una imagen (JPG, PNG)");
        }

        // Validar tamaños (máximo 5MB cada uno)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (cedula.getSize() > maxSize || foto.getSize() > maxSize || prerrequisitos.getSize() > maxSize) {
            throw new RuntimeException("Los archivos no deben superar 5MB cada uno");
        }
    }

    private void guardarDocumento(Long idPrepostulacion, MultipartFile archivo, String tipoNombre) throws IOException {
        // Obtener o crear tipo de documento
        TipoDocumento tipoDocumento = tipoDocumentoRepository.findByNombre(tipoNombre)
                .orElseGet(() -> {
                    TipoDocumento nuevo = new TipoDocumento();
                    nuevo.setNombre(tipoNombre);
                    nuevo.setDescripcion("Documento tipo " + tipoNombre);
                    nuevo.setObligatorio(true);
                    nuevo.setActivo(true);
                    return tipoDocumentoRepository.save(nuevo);
                });

        // Guardar archivo físico
        String subfolder = tipoNombre.toLowerCase().replace("_", "-");
        String rutaArchivo = fileStorageService.guardarArchivo(archivo, subfolder);

        // Crear registro en documento_temporal
        DocumentoTemporal documento = new DocumentoTemporal();
        documento.setIdPrepostulacion(idPrepostulacion);
        documento.setIdTipoDocumento(tipoDocumento.getIdTipoDocumento());
        documento.setRutaArchivo(rutaArchivo);
        documento.setNombreOriginal(archivo.getOriginalFilename());
        documento.setTamanoBytes(archivo.getSize());

        documentoTemporalRepository.save(documento);
    }
}