package org.uteq.backend.Service;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.uteq.backend.Repository.IAuditLoginRepository;
import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.AuditLoginResultado;

@Service
public class LoginAuditServiceImpl implements ILoginAuditService {

    private final IAuditLoginRepository auditLoginRepository;

    public LoginAuditServiceImpl(IAuditLoginRepository auditLoginRepository) {
        this.auditLoginRepository = auditLoginRepository;
    }

    @Override
    public void logSuccess(String usuarioApp, String usuarioBd, HttpServletRequest request) {
        try {
            auditLoginRepository.auditLogin(
                    usuarioApp,
                    usuarioBd,
                    AuditLoginResultado.SUCCESS,
                    null,
                    getClientIp(request),
                    getUserAgent(request)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void logFail(String usuarioApp, String usuarioBdOrNull, AuditLoginMotivo motivo, HttpServletRequest request) {
        try {
            auditLoginRepository.auditLogin(
                    usuarioApp,
                    usuarioBdOrNull,
                    AuditLoginResultado.FAIL,
                    motivo,
                    getClientIp(request),
                    getUserAgent(request)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return (ua == null || ua.isBlank()) ? null : ua;
    }

    private String getClientIp(HttpServletRequest request) {
        // Si hay proxy inverso, esta es la IP real (tomamos la primera)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String first = xff.split(",")[0].trim();
            return normalizeIp(first);
        }
        return normalizeIp(request.getRemoteAddr());
    }

    // Evita valores que rompan el cast ?::inet en PostgreSQL
    private String normalizeIp(String ip) {
        if (ip == null) return null;

        ip = ip.trim();

        // Caso común: IPv6 loopback
        if ("0:0:0:0:0:0:0:1".equals(ip)) return "::1";

        // Si viene con puerto (ej: "190.12.1.2:52341"), quita el puerto
        int colonCount = ip.length() - ip.replace(":", "").length();
        if (colonCount == 1 && ip.contains(".") && ip.contains(":")) {
            // IPv4:puerto
            ip = ip.substring(0, ip.indexOf(':'));
        }

        return ip;
    }
}
