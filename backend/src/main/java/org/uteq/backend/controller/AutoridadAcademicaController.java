package org.uteq.backend.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.uteq.backend.service.AutoridadAcademicaService;
import org.uteq.backend.dto.AutoridadAcademicaRequestDTO;
import org.uteq.backend.dto.AutoridadAcademicaResponseDTO;
import org.uteq.backend.dto.AutoridadEstadoRequestDTO;
import org.uteq.backend.dto.AutoridadRegistroRequestDTO;
import org.uteq.backend.dto.AutoridadDesdeUsuarioRequestDTO;
import org.uteq.backend.repository.UsuarioRepository;

@RestController
@RequestMapping("/api/autoridades-academicas")
@CrossOrigin
public class AutoridadAcademicaController {

    private final AutoridadAcademicaService autoridadService;
    private final UsuarioRepository usuarioRepository;

    public AutoridadAcademicaController(AutoridadAcademicaService autoridadService,
                                        UsuarioRepository usuarioRepository) {
        this.autoridadService = autoridadService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public AutoridadAcademicaResponseDTO crear(@RequestBody AutoridadAcademicaRequestDTO dto) {
        return autoridadService.crear(dto);
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody AutoridadRegistroRequestDTO dto) {
        System.out.println("idInstitucion=" + dto.getIdInstitucion());
        System.out.println("idsRolAutoridad=" + dto.getIdsRolAutoridad());
        return ResponseEntity.ok(autoridadService.registrarAutoridad(dto));
    }

    @GetMapping
    public List<AutoridadAcademicaResponseDTO> listar() {
        return autoridadService.listar();
    }

    @GetMapping("/{id}")
    public AutoridadAcademicaResponseDTO obtener(@PathVariable Long id) {
        return autoridadService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public AutoridadAcademicaResponseDTO actualizar(
            @PathVariable Long id,
            @RequestBody AutoridadAcademicaRequestDTO dto) {
        return autoridadService.actualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        autoridadService.eliminar(id);
    }

    @PatchMapping("/{idAutoridad}/estado")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable Long idAutoridad,
            @RequestBody @Valid AutoridadEstadoRequestDTO dto
    ) {
        autoridadService.cambiarEstado(idAutoridad, dto.estado());
        return ResponseEntity.ok().build();
    }

    /** Crea autoridad vinculando un usuario existente (no postulante ni autoridad ya). */
    @PostMapping("/desde-usuario")
    public ResponseEntity<?> registrarDesdeUsuario(@RequestBody AutoridadDesdeUsuarioRequestDTO dto) {
        return ResponseEntity.ok(autoridadService.registrarDesdeUsuario(dto));
    }

    /** Lista usuarios disponibles (no son autoridad ni postulante) para vincular. */
    @GetMapping("/usuarios-disponibles")
    public ResponseEntity<?> usuariosDisponibles() {
        var usuarios = usuarioRepository.findUsuariosDisponiblesParaAutoridad();
        var result = usuarios.stream().map(u -> {
            var map = new java.util.LinkedHashMap<String, Object>();
            map.put("idUsuario", u.getIdUsuario());
            map.put("usuarioApp", u.getUsuarioApp());
            map.put("correo", u.getCorreo());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

}