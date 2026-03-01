// entrevistas-docentes/shared/evaluacion.constants.ts
// Constantes del módulo de evaluación docente

// ─── API ─────────────────────────────────────────────────────────────────────

export const API_BASE = 'http://localhost:8080/api/evaluacion';

export const API_ENDPOINTS = {
  fases:       `${API_BASE}/fases`,
  plantillas:  `${API_BASE}/plantillas`,
  criterios:   `${API_BASE}/criterios`,
  procesos:    `${API_BASE}/procesos`,
  reuniones:   `${API_BASE}/reuniones`,
  evaluaciones:`${API_BASE}/evaluaciones`,
  dashboard:   `${API_BASE}/procesos/dashboard`,
} as const;

// ─── TIPOS DE FASE ────────────────────────────────────────────────────────────

export const TIPOS_FASE = [
  { value: 'automatica', label: 'Automática (Revisión Documental)' },
  { value: 'reunion',    label: 'Reunión de Evaluación'            },
  { value: 'practica',   label: 'Actividad Práctica'               },
  { value: 'decision',   label: 'Decisión de Comité'               },
] as const;

// ─── MODALIDADES DE REUNIÓN ───────────────────────────────────────────────────

export const MODALIDADES_REUNION = [
  { value: 'zoom',       label: 'Zoom'               },
  { value: 'meet',       label: 'Google Meet'        },
  { value: 'teams',      label: 'Microsoft Teams'    },
  { value: 'presencial', label: 'Presencial'         },
] as const;

// ─── DURACIONES ───────────────────────────────────────────────────────────────

export const DURACIONES_REUNION = [
  { value: 30,  label: '30 minutos' },
  { value: 60,  label: '1 hora'     },
  { value: 90,  label: '1.5 horas'  },
  { value: 120, label: '2 horas'    },
] as const;

// ─── ESCALAS DE CRITERIOS ─────────────────────────────────────────────────────

export const ESCALAS_CRITERIO = [
  { value: '1-5',   label: '1 a 5 estrellas', max: 5   },
  { value: '1-10',  label: '1 a 10 puntos',   max: 10  },
  { value: '0-100', label: '0 a 100 puntos',  max: 100 },
] as const;

// ─── DECISIONES FINALES ───────────────────────────────────────────────────────

export const DECISIONES_FINALES = [
  { value: 'aprobado_contratar', label: 'Aprobado – Contratar'        },
  { value: 'aprobado_espera',    label: 'Aprobado – Lista de Espera'   },
  { value: 'no_aprobado',        label: 'No Aprobado'                  },
  { value: 'segunda_ronda',      label: 'Requiere Segunda Ronda'       },
] as const;

// ─── EVALUADORES DISPONIBLES ──────────────────────────────────────────────────

export const EVALUADORES_PERMITIDOS = [
  'Comité de Selección',
  'Coordinador',
  'Experto Técnico',
  'Decano',
  'Pedagogo',
  'Comité Evaluador',
  'Vicerrectorado',
] as const;

// ─── LABELS DE ESTADO ────────────────────────────────────────────────────────

export const ESTADO_PROCESO_LABELS: Record<string, string> = {
  en_proceso: 'En Proceso',
  completado: 'Completado',
  rechazado:  'Rechazado',
  pendiente:  'Pendiente',
};

export const ESTADO_FASE_LABELS: Record<string, string> = {
  completada: 'Completada',
  en_curso:   'En Curso',
  pendiente:  'Pendiente',
  bloqueada:  'Bloqueada',
};

export const ESTADO_REUNION_LABELS: Record<string, string> = {
  programada: 'Programada',
  en_curso:   'En Curso',
  completada: 'Completada',
  cancelada:  'Cancelada',
};

// ─── CSS BADGE CLASSES ────────────────────────────────────────────────────────

export const ESTADO_PROCESO_BADGE: Record<string, string> = {
  en_proceso: 'warning',
  completado: 'success',
  rechazado:  'danger',
  pendiente:  'pending',
};

export const ESTADO_FASE_BADGE: Record<string, string> = {
  completada: 'success',
  en_curso:   'warning',
  pendiente:  'info',
  bloqueada:  'default',
};

export const TIPO_FASE_BADGE: Record<string, string> = {
  automatica: 'info',
  reunion:    'warning',
  practica:   'pending',
  decision:   'danger',
};

// ─── PAGINACIÓN ───────────────────────────────────────────────────────────────

export const PAGE_SIZE_DEFAULT = 10;

// ─── FIRMA DIGITAL ────────────────────────────────────────────────────────────

export const FIRMA_CONFIG = {
  strokeStyle: '#016630',
  lineWidth:   2,
  lineCap:     'round' as CanvasLineCap,
  width:       700,
  height:      180,
} as const;
