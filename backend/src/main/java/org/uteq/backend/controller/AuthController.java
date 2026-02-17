package org.uteq.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uteq.backend.entity.Usuario;
import org.uteq.backend.service.DbSwitchService;
import org.uteq.backend.dto.LoginRequest;
import org.uteq.backend.dto.LoginResponse;
import org.uteq.backend.service.AuthService;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private DbSwitchService dbSwitchService;
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            LoginResponse response = authService.login(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Credenciales inválidas");

        }
    }
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // vuelve a conexión backend base
        dbSwitchService.resetToDefault();
        System.out.println("Deslogueando");

        return ResponseEntity.ok("Logout OK (conexión reseteada)");
    }


}