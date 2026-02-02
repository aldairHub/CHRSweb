package org.uteq.backend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.UsuarioCreateDTO;
import org.uteq.backend.dto.UsuarioDTO;
import org.uteq.backend.dto.UsuarioUpdateDTO;
import org.uteq.backend.Service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    // Crear usuario (solo ADMIN o EVALUATOR)
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioCreateDTO dto) {
        try {
            UsuarioDTO usuario = usuarioService.crear(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Listar todos
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    // Listar solo autoridades (ADMIN y EVALUATOR)
    @GetMapping("/autoridades")
    public ResponseEntity<List<UsuarioDTO>> listarAutoridades() {
        return ResponseEntity.ok(usuarioService.listarAutoridades());
    }

    // Obtener por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        try {
            UsuarioDTO usuario = usuarioService.obtenerPorId(id);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    // Actualizar
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody UsuarioUpdateDTO dto) {
        try {
            UsuarioDTO usuario = usuarioService.actualizar(id, dto);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // Activar/Desactivar
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam Boolean activo) {
        try {
            UsuarioDTO usuario = usuarioService.cambiarEstado(id, activo);
            return ResponseEntity.ok(usuario);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}