// services/entrevistas-models.ts
// Interfaces compartidas — espejo exacto de los DTO del backend

// ─── FASES ───────────────────────────────────────────────────────────────────

export interface FaseRequest {
  nombre: string;
  tipo: 'automatica' | 'reunion' | 'practica' | 'decision';
  peso: number;
  orden: number;
  evaluadoresPermitidos: string[];
  estado?: boolean;
}

export interface FaseResponse {
  idFase: number;
  nombre: string;
  tipo: 'automatica' | 'reunion' | 'practica' | 'decision';
  peso: number;
  orden: number;
  evaluadoresPermitidos: string[];
  estado: boolean;
  numeroPlantillas: number;
}

// ─── PLANTILLAS ───────────────────────────────────────────────────────────────

export interface PlantillaRequest {
  codigo: string;
  nombre: string;
  idFase: number;
  estado?: boolean;
}

export interface PlantillaResponse {
  idPlantilla: number;
  codigo: string;
  nombre: string;
  idFase: number;
  nombreFase: string;
  numeroCriterios: number;
  ultimaModificacion: string;
  estado: boolean;
}

// ─── CRITERIOS ────────────────────────────────────────────────────────────────

export interface CriterioRequest {
  nombre: string;
  descripcion: string;
  peso: number;
  escala: '1-5' | '1-10' | '0-100';
  rubrica?: string;
  idPlantilla: number;
}

export interface CriterioResponse {
  idCriterio: number;
  nombre: string;
  descripcion: string;
  peso: number;
  escala: '1-5' | '1-10' | '0-100';
  rubrica?: string;
  idPlantilla: number;
}

export interface PesoTotalResponse {
  idPlantilla: number;
  pesoTotal: number;
  valido: boolean;
}

// ─── POSTULANTES / PROCESOS ───────────────────────────────────────────────────

export interface PostulanteResumen {
  idProceso: number;
  idPostulante: number;
  codigo: string;
  nombres: string;
  apellidos: string;
  cedula: string;
  materia: string;
  faseActual: string;
  progreso: number;
  estadoGeneral: 'en_proceso' | 'completado' | 'rechazado' | 'pendiente';
}

export interface FaseProcesoDetalle {
  idFase: number;
  orden: number;
  nombre: string;
  peso: number;
  estado: 'completada' | 'en_curso' | 'pendiente' | 'bloqueada';
  calificacion?: number;
  fechaCompletada?: string;
  reunion?: ReunionResumen;
  evaluadores?: string[];
}

export interface HistorialAccion {
  fecha: string;
  titulo: string;
  descripcion: string;
  usuario?: string;
}

export interface PostulanteDetalle extends PostulanteResumen {
  fases: FaseProcesoDetalle[];
  historial: HistorialAccion[];
}

export interface CrearProcesoRequest {
  idPostulante: number;
  idSolicitud: number;
}

// ─── REUNIONES ────────────────────────────────────────────────────────────────

export interface ReunionRequest {
  idProceso: number;
  idFase: number;
  fecha: string;        // yyyy-MM-dd
  hora: string;         // HH:mm
  duracionMinutos: number;
  modalidad: 'zoom' | 'meet' | 'teams' | 'presencial';
  enlace?: string;
  evaluadoresIds: number[];
  observaciones?: string;
}

export interface ReunionResumen {
  idReunion: number;
  idProceso: number;
  idFase: number;
  fecha: string;
  hora: string;
  duracionMinutos: number;
  modalidad: 'zoom' | 'meet' | 'teams' | 'presencial';
  enlace?: string;
  evaluadores: string[];
  estado: 'programada' | 'en_curso' | 'completada' | 'cancelada';
}

// ─── EVALUACIONES ─────────────────────────────────────────────────────────────

export interface CriterioEvaluadoRequest {
  idCriterio: number;
  nota: number;
  observacion?: string;
}

export interface EvaluacionRequest {
  idReunion: number;
  criterios: CriterioEvaluadoRequest[];
  observaciones: string;
  declaroSinConflicto: boolean;
  firmaDigital: string;
}

export interface CriterioEvaluadoResponse {
  idCriterio: number;
  nombre: string;
  peso: number;
  nota: number;
  observacion?: string;
}

export interface EvaluacionResponse {
  idEvaluacion: number;
  idReunion: number;
  idEvaluador: number;
  nombreEvaluador: string;
  criterios: CriterioEvaluadoResponse[];
  observaciones: string;
  calificacionFinal: number;
  fechaEvaluacion: string;
  firmaDigital?: string;
  confirmada: boolean;
}

// ─── RESULTADOS ───────────────────────────────────────────────────────────────

export interface ResultadoFase {
  idFase: number;
  nombreFase: string;
  peso: number;
  calificacion?: number;
  ponderado?: number;
  evaluadores: string[];
  estado: 'completada' | 'programada' | 'bloqueada' | 'pendiente';
  evaluaciones?: EvaluacionResponse[];
}

export interface ResultadoProceso {
  idProceso: number;
  idPostulante: number;
  nombreCompleto: string;
  materia: string;
  fasesResultados: ResultadoFase[];
  calificacionTotal: number;
  progreso: number;
  decision?: 'aprobado_contratar' | 'aprobado_espera' | 'no_aprobado' | 'segunda_ronda';
  justificacionDecision?: string;
}

export interface DecisionFinalRequest {
  decision: string;
  justificacion: string;
}

// ─── DASHBOARD ────────────────────────────────────────────────────────────────

export interface DashboardStats {
  postulantesActivos: number;
  reunionesProgramadas: number;
  evaluacionesCompletas: number;
  pendientesHoy: number;
}
