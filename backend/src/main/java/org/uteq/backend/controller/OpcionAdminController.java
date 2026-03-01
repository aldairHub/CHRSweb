package org.uteq.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.dto.AsignarOpcionDTO;
import org.uteq.backend.dto.CrearOpcionDTO;
import org.uteq.backend.repository.PostgresProcedureRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/opciones")
public class OpcionAdminController {

    private final PostgresProcedureRepository procedureRepo;

    public OpcionAdminController(PostgresProcedureRepository procedureRepo) {
        this.procedureRepo = procedureRepo;
    }

    // Opciones del módulo del rol con flags (para la pantalla de gestión)
    @GetMapping("/rol/{idRolApp}")
    public ResponseEntity<?> opcionesDeRol(@PathVariable Integer idRolApp) {
        return ResponseEntity.ok(
                procedureRepo.opcionesRolAppConFlag(idRolApp));
    }

    // Listar módulos (para el dropdown del formulario de rol)
    @GetMapping("/modulos")
    public ResponseEntity<?> listarModulos() {
        return ResponseEntity.ok(procedureRepo.listarModulos());
    }

    // Crear opción nueva
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CrearOpcionDTO dto) {
        Integer id = procedureRepo.crearOpcion(
                dto.getNombreModulo(), dto.getNombre(),
                dto.getDescripcion(), dto.getRuta(), dto.getOrden());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("idOpcion", id));
    }

    // Asignar opción a rol (crea o actualiza solo_lectura)
    @PostMapping("/asignar")
    public ResponseEntity<?> asignar(@RequestBody AsignarOpcionDTO dto) {
        procedureRepo.asignarOpcionARol(
                dto.getIdRolApp(), dto.getIdOpcion(),
                dto.getSoloLectura() != null ? dto.getSoloLectura() : false);
        return ResponseEntity.ok().build();
    }

    // Quitar opción de rol
    @DeleteMapping("/quitar")
    public ResponseEntity<?> quitar(@RequestBody AsignarOpcionDTO dto) {
        procedureRepo.quitarOpcionDeRol(
                dto.getIdRolApp(), dto.getIdOpcion());
        return ResponseEntity.ok().build();
    }
}
