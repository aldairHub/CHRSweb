package org.uteq.backend.service;

import org.springframework.stereotype.Service;
import org.uteq.backend.dto.TipoDocumentoDTO;
import org.uteq.backend.repository.TipoDocumentoRepositoryCustom;

import java.util.List;
import java.util.Map;

@Service
public class TipoDocumentoService {

    private final TipoDocumentoRepositoryCustom repo;

    public TipoDocumentoService(TipoDocumentoRepositoryCustom repo) {
        this.repo = repo;
    }

    public List<TipoDocumentoDTO> listar() {
        return repo.listar();
    }

    public Map<String, Object> crear(String nombre, String descripcion, Boolean obligatorio) {
        return repo.crear(nombre, descripcion, obligatorio);
    }

    public Map<String, Object> editar(Long id, String nombre, String descripcion, Boolean obligatorio) {
        return repo.editar(id, nombre, descripcion, obligatorio);
    }

    public Map<String, Object> toggle(Long id) {
        return repo.toggle(id);
    }
}