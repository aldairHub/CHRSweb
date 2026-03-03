// entrevistas-docentes/shared/evaluacion.models.ts
// Interfaces del módulo — alineadas 100% con los DTOs del backend Spring Boot

// ─── FASES ────────────────────────────────────────────────────────────────────

export interface FaseDTO {
  idFase: number;
  nombre: string;
  tipo: 'automatica' | 'reunion' | 'practica' | 'decision';
  peso: number;
  orden: number;
  evaluadoresPermitidos: string[];
  estado: boolean;
  numeroPlantillas: number;
}

export interface FaseCreateDTO {
  nombre: string;
  tipo: 'automatica' | 'reunion' | 'practica' | 'decision';
  peso: number;
  orden: number;
  evaluadoresPermitidos: string[];
  estado?: boolean;
}

// ─── PLANTILLAS ───────────────────────────────────────────────────────────────

export interface PlantillaDTO {
  idPlantilla: number;
  codigo: string;
  nombre: string;
  idFase: number;
  nombreFase: string;
  numeroCriterios: number;
  ultimaModificacion: string; // formato dd/MM/yyyy desde el backend
  estado: boolean;
}

export interface PlantillaCreateDTO {
  codigo: string;
  nombre: string;
  idFase: number;
  estado?: boolean;
}

// ─── CRITERIOS ────────────────────────────────────────────────────────────────

export interface CriterioDTO {
  idCriterio: number;
  nombre: string;
  descripcion: string;
  peso: number;
  escala: '1-5' | '1-10' | '0-100';
  rubrica?: string;
  idPlantilla: number;
}

export interface CriterioCreateDTO {
  nombre: string;
  descripcion: string;
  peso: number;
  escala: '1-5' | '1-10' | '0-100';
  rubrica?: string;
  idPlantilla: number;
}

export interface PesoTotalDTO {
  idPlantilla: number;
  pesoTotal: number;
  valido: boolean;
}

// ─── PROCESOS / POSTULANTES ───────────────────────────────────────────────────

/**
 * IMPORTANTE: El backend trabaja con idProceso como identificador principal,
 * no idPostulante. Usar idProceso para navegar y hacer peticiones.
 */
export interface PostulanteDTO {
  idProceso: number;      // ID del ProcesoEvaluacionPostulante — usar ESTE para las peticiones
  idPostulante: number;   // ID del postulante original
  codigo: string;
  nombres: string;
  apellidos: string;
  cedula: string;
  materia: string;
  faseActual: string;
  progreso: number;
  estadoGeneral: 'en_proceso' | 'completado' | 'rechazado' | 'pendiente';
}

export interface PostulanteDetalleDTO extends PostulanteDTO {
  fases: FaseProcesoDTO[];
  historial: HistorialAccionDTO[];
}

export interface CrearProcesoDTO {
  idPostulante: number;
  idSolicitud: number;
}

// ─── FASES DEL PROCESO ────────────────────────────────────────────────────────

export interface FaseProcesoDTO {
  idFase: number;
  orden: number;
  nombre: string;
  peso: number;
  estado: 'completada' | 'en_curso' | 'pendiente' | 'bloqueada';
  calificacion?: number;
  fechaCompletada?: string;
  reunion?: ReunionDTO;
  evaluadores?: string[];
}

export interface HistorialAccionDTO {
  fecha: string;
  titulo: string;
  descripcion: string;
  usuario?: string;
}

// ─── REUNIONES ────────────────────────────────────────────────────────────────

export interface ReunionDTO {
  idReunion: number;
  idProceso: number;         // Antes era idPostulante — ahora es idProceso
  idFase: number;
  fecha: string;             // yyyy-MM-dd
  hora: string;              // HH:mm
  duracionMinutos: number;   // Antes era "duracion" — ahora es duracionMinutos
  modalidad: 'zoom' | 'meet' | 'teams' | 'presencial';
  enlace?: string;
  evaluadores: string[];
  observaciones?: string;
  estado: 'programada' | 'en_curso' | 'completada' | 'cancelada';
}

export interface ReunionCreateDTO {
  idProceso: number;
  idFase: number;
  fecha: string;
  hora: string;
  duracionMinutos: number;
  modalidad: 'zoom' | 'meet' | 'teams' | 'presencial';
  enlace?: string;
  evaluadoresIds: number[];
  observaciones?: string;
}

// ─── EVALUACIONES ─────────────────────────────────────────────────────────────

export interface EvaluacionDTO {
  idEvaluacion: number;
  idReunion: number;
  idEvaluador: number;
  nombreEvaluador: string;
  criterios: CriterioEvaluadoDTO[];
  observaciones: string;
  calificacionFinal: number;
  fechaEvaluacion: string;
  firmaDigital?: string;
  confirmada: boolean;
}

export interface CriterioEvaluadoDTO {
  idCriterio: number;
  nombre: string;
  peso: number;
  nota: number;
  observacion?: string;
}

export interface EvaluacionCreateDTO {
  idReunion: number;
  criterios: { idCriterio: number; nota: number; observacion?: string }[];
  observaciones: string;
  declaroSinConflicto: boolean;
  firmaDigital: string;
}

// ─── RESULTADOS ───────────────────────────────────────────────────────────────

export interface ResultadoFaseDTO {
  idFase: number;
  nombreFase: string;
  peso: number;
  calificacion?: number;
  ponderado?: number;
  evaluadores: string[];
  estado: 'completada' | 'programada' | 'bloqueada' | 'pendiente';
  evaluaciones?: EvaluacionDTO[];
}

export interface ResultadoProcesoDTO {
  idProceso: number;
  idPostulante: number;
  nombreCompleto: string;
  materia: string;
  fasesResultados: ResultadoFaseDTO[];
  calificacionTotal: number;
  progreso: number;
  decision?: 'aprobado_contratar' | 'aprobado_espera' | 'no_aprobado' | 'segunda_ronda';
  justificacionDecision?: string;
}

export interface DecisionFinalDTO {
  decision: string;
  justificacion: string;
}

// ─── DASHBOARD ────────────────────────────────────────────────────────────────

export interface DashboardStatsDTO {
  postulantesActivos: number;
  reunionesProgramadas: number;
  evaluacionesCompletas: number;
  pendientesHoy: number;
}
