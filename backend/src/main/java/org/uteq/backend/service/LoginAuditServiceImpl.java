package org.uteq.backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.uteq.backend.repository.AuditLoginRepository;
import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.AuditLoginResultado;

@Service
public class LoginAuditServiceImpl implements LoginAuditService {

    private final AuditLoginRepository auditLoginRepository;

    public LoginAuditServiceImpl(AuditLoginRepository auditLoginRepository) {
        this.auditLoginRepository = auditLoginRepository;
    }

    @Override
    public void logSuccess(String usuarioApp, String usuarioBd,
                           Long idUsuario, HttpServletRequest request) {
        auditLoginRepository.auditLogin(
                usuarioApp, usuarioBd,
                AuditLoginResultado.SUCCESS,
                null,
                getClientIp(request), getUserAgent(request),
                idUsuario
        );
    }

    @Override
    public void logFail(String usuarioApp, String usuarioBdOrNull,
                        Long idUsuarioOrNull, AuditLoginMotivo motivo,
                        HttpServletRequest request) {
        auditLoginRepository.auditLogin(
                usuarioApp, usuarioBdOrNull,
                AuditLoginResultado.FAIL,
                motivo,
                getClientIp(request), getUserAgent(request),
                idUsuarioOrNull
        );
    }

    @Override
    public void logLogout(String usuarioApp, String usuarioBd,
                          Long idUsuario, HttpServletRequest request) {
        auditLoginRepository.auditLogin(
                usuarioApp, usuarioBd,
                AuditLoginResultado.SUCCESS,
                AuditLoginMotivo.LOGOUT,
                getClientIp(request), getUserAgent(request),
                idUsuario
        );
    }

    // ─── Helpers ───────────────────────────────────────────────

    private String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return (ua == null || ua.isBlank()) ? null : ua;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return normalizeIp(xff.split(",")[0].trim());
        }
        return normalizeIp(request.getRemoteAddr());
    }

    private String normalizeIp(String ip) {
        if (ip == null) return null;
        ip = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(ip)) return "::1";
        int colonCount = ip.length() - ip.replace(":", "").length();
        if (colonCount == 1 && ip.contains(".") && ip.contains(":")) {
            ip = ip.substring(0, ip.indexOf(':'));
        }
        return ip;
    }
}