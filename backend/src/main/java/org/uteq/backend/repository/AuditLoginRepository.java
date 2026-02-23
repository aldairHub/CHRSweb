package org.uteq.backend.repository;

import org.uteq.backend.dto.AuditLoginMotivo;
import org.uteq.backend.dto.AuditLoginResultado;

public interface AuditLoginRepository {
    void auditLogin(
            String usuarioApp,
            String usuarioBd,
            AuditLoginResultado resultado,
            AuditLoginMotivo motivo,
            String ipCliente,
            String userAgent,
            Long idUsuario  //  nuevo
    );
}