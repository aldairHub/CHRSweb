package org.uteq.backend.Service;

import jakarta.servlet.http.HttpServletRequest;
import org.uteq.backend.dto.AuditLoginMotivo;

public interface ILoginAuditService {
    void logSuccess(String usuarioApp, String usuarioBd, HttpServletRequest request);
    void logFail(String usuarioApp, String usuarioBdOrNull, AuditLoginMotivo motivo, HttpServletRequest request);
}
