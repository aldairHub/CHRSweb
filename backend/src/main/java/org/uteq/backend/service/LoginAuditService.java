package org.uteq.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.uteq.backend.dto.AuditLoginMotivo;


public interface LoginAuditService {

    void logSuccess(
            String usuarioApp,
            String usuarioBd,
            Long idUsuario,         // ✅ nuevo
            HttpServletRequest request
    );

    void logFail(
            String usuarioApp,
            String usuarioBdOrNull,
            Long idUsuarioOrNull,   // ✅ nuevo
            AuditLoginMotivo motivo,
            HttpServletRequest request
    );

    void logLogout(             // ✅ nuevo método
                                String usuarioApp,
                                String usuarioBd,
                                Long idUsuario,
                                HttpServletRequest request
    );}
