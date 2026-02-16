package org.uteq.backend.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CodigoVerificacionService {

    private static class CodigoInfo {
        String codigo;
        LocalDateTime expiracion;
    }

    private final Map<String, CodigoInfo> codigos =
            new ConcurrentHashMap<>();

    public String generarCodigo(String correo) {
        String codigo =
                String.valueOf((int)(Math.random() * 900000) + 100000);

        CodigoInfo info = new CodigoInfo();
        info.codigo = codigo;
        info.expiracion = LocalDateTime.now().plusMinutes(10);

        codigos.put(correo, info);
        return codigo;
    }

    public boolean validarCodigo(String correo, String codigo) {
        CodigoInfo info = codigos.get(correo);

        if (info == null) return false;
        if (LocalDateTime.now().isAfter(info.expiracion)) return false;

        return info.codigo.equals(codigo);
    }
}