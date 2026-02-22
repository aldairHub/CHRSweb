package org.uteq.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String usuarioApp;
    private Set<String> roles;
    private Boolean primerLogin;
}
