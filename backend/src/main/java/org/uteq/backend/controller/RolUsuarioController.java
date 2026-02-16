//package org.uteq.backend.controller;
//
//import org.springframework.web.bind.annotation.CrossOrigin;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.uteq.backend.repository.IRolUsuarioRepository;
//import org.uteq.backend.dto.RolUsuarioResponseDTO;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/roles-usuario")
//@CrossOrigin(origins = "*")
//public class RolUsuarioController {
//
//    private final IRolUsuarioRepository repo;
//
//    public RolUsuarioController(IRolUsuarioRepository repo) {
//        this.repo = repo;
//    }
//
//    @GetMapping
//    public List<RolUsuarioResponseDTO> listar() {
//        return repo.findAllByOrderByNombreAsc()
//                .stream()
//                .map(r -> new RolUsuarioResponseDTO(r.getIdRolUsuario(), r.getNombre()))
//                .toList();
//    }
//}
