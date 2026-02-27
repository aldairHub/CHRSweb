package org.uteq.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.uteq.backend.dto.DashboardEvaluacionDTO;
import org.uteq.backend.repository.EvaluacionRepository;
import org.uteq.backend.repository.ProcesoEvaluacionRepository;
import org.uteq.backend.repository.ReunionRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardEvaluacionService {

    private final ProcesoEvaluacionRepository procesoRepository;
    private final ReunionRepository reunionRepository;
    private final EvaluacionRepository evaluacionRepository;

    @Transactional(readOnly = true)
    public DashboardEvaluacionDTO obtenerStats() {
        return DashboardEvaluacionDTO.builder()
                .postulantesActivos(procesoRepository.countActivos())
                .reunionesProgramadas(reunionRepository.countProgramadas())
                .evaluacionesCompletas(evaluacionRepository.countConfirmadas())
                .pendientesHoy(reunionRepository.countByFecha(LocalDate.now()))
                .build();
    }
}
