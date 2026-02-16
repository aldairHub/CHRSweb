package org.uteq.backend.repository;

import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.AuditLoginResultado;

public interface IAuditLoginRepository {
    void auditLogin(
            String usuarioApp,
            String usuarioBd,
            AuditLoginResultado resultado,
            AuditLoginMotivo motivo,
            String ipCliente,
            String userAgent
    );
}