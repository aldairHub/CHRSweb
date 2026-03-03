// entrevistas-docentes/shared/evaluacion.enums.ts
// Enums del módulo — valores exactos que acepta el backend

export enum TipoFase {
  AUTOMATICA = 'automatica',
  REUNION    = 'reunion',
  PRACTICA   = 'practica',
  DECISION   = 'decision'
}

export enum EstadoProceso {
  EN_PROCESO = 'en_proceso',
  COMPLETADO = 'completado',
  RECHAZADO  = 'rechazado',
  PENDIENTE  = 'pendiente'
}

export enum EstadoFaseProceso {
  COMPLETADA = 'completada',
  EN_CURSO   = 'en_curso',
  PENDIENTE  = 'pendiente',
  BLOQUEADA  = 'bloqueada'
}

export enum ModalidadReunion {
  ZOOM       = 'zoom',
  MEET       = 'meet',
  TEAMS      = 'teams',
  PRESENCIAL = 'presencial'
}

export enum EstadoReunion {
  PROGRAMADA = 'programada',
  EN_CURSO   = 'en_curso',
  COMPLETADA = 'completada',
  CANCELADA  = 'cancelada'
}

export enum EscalaCriterio {
  UNO_CINCO    = '1-5',
  UNO_DIEZ     = '1-10',
  CERO_CIEN    = '0-100'
}

export enum EstadoResultadoFase {
  COMPLETADA = 'completada',
  PROGRAMADA = 'programada',
  BLOQUEADA  = 'bloqueada',
  PENDIENTE  = 'pendiente'
}

export enum DecisionFinal {
  APROBADO_CONTRATAR = 'aprobado_contratar',
  APROBADO_ESPERA    = 'aprobado_espera',
  NO_APROBADO        = 'no_aprobado',
  SEGUNDA_RONDA      = 'segunda_ronda'
}
