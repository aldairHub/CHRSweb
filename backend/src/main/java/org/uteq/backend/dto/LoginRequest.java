package org.uteq.backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String usuarioApp;
    private String claveApp;
}
