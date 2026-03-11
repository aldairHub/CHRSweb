import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ========== INTERFACES ==========

export interface SolicitudDocenteRequest {
  idCarrera: number;
  idMateria: number;
  idArea: number;
  cantidadDocentes: number;
  nivelAcademico: string;
  experienciaProfesionalMin: number;
  experienciaDocenteMin: number;
  justificacion: string;
  observaciones?: string;
}

export interface SolicitudDocenteResponse {
  idSolicitud: number;
  idAutoridad: number;
  nombreAutoridad: string;
  idCarrera: number;
  nombreCarrera: string;
  modalidadCarrera: string;
  idFacultad: number;
  nombreFacultad: string;
  idMateria: number;
  nombreMateria: string;
  nivelMateria: number;
  idArea: number;
  nombreArea: string;
  fechaSolicitud: string;
  estadoSolicitud: string;
  justificacion: string;
  cantidadDocentes: number;
  nivelAcademico: string;
  experienciaProfesionalMin: number;
  experienciaDocenteMin: number;
  observaciones?: string;
}

export interface Carrera {
  idCarrera: number;
  nombreCarrera: string;
  modalidad: string;
  estado: boolean;
  idFacultad?: number;
}

export interface Materia {
  idMateria: number;
  nombre: string;
  nivel: number;
}

export interface AreaConocimiento {
  idArea: number;
  nombreArea: string;
}

export interface SolicitudConUsuario {
  usuarioApp: string;
  solicitud: SolicitudDocenteRequest;
}

// LEGACY: mantener por compatibilidad
export interface SolicitudConCredenciales {
  usuarioBd: string;
  claveBd: string;
  solicitud: SolicitudDocenteRequest;
}

@Injectable({
  providedIn: 'root'
})
export class SolicitudDocenteService {

  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // ── NUEVO: Obtener idFacultad del evaluador logueado ─────────────────
  // Llama a GET /api/solicitudes-docente/mi-facultad?usuarioApp=...
  // El backend lee AutoridadAcademica.idFacultad del usuario autenticado
  obtenerMiFacultad(usuarioApp: string): Observable<{ idFacultad: number }> {
    return this.http.get<{ idFacultad: number }>(
      `${this.apiUrl}/solicitudes-docente/mi-facultad`,
      { params: { usuarioApp } }
    );
  }

  // ── Carreras filtradas por facultad ──────────────────────────────────
  // Llama a GET /api/carreras/por-facultad/{idFacultad}
  obtenerCarrerasPorFacultad(idFacultad: number): Observable<Carrera[]> {
    return this.http.get<Carrera[]>(`${this.apiUrl}/carreras/por-facultad/${idFacultad}`);
  }

  // ── Materias por carrera ──────────────────────────────────────────────
  obtenerMateriasPorCarrera(idCarrera: number): Observable<Materia[]> {
    return this.http.get<Materia[]>(`${this.apiUrl}/materias/carrera/${idCarrera}`);
  }

  // ── Áreas de conocimiento ─────────────────────────────────────────────
  obtenerAreasConocimiento(): Observable<AreaConocimiento[]> {
    return this.http.get<AreaConocimiento[]>(`${this.apiUrl}/areas-conocimiento`);
  }

  // ── Crear solicitud ───────────────────────────────────────────────────
  crearSolicitud(request: SolicitudConUsuario): Observable<SolicitudDocenteResponse> {
    return this.http.post<SolicitudDocenteResponse>(
      `${this.apiUrl}/solicitudes-docente`,
      request
    );
  }

  // LEGACY
  crearSolicitudConCredenciales(request: SolicitudConCredenciales): Observable<SolicitudDocenteResponse> {
    return this.crearSolicitud({
      usuarioApp: request.usuarioBd,
      solicitud:  request.solicitud
    });
  }

  // ── Consultas ─────────────────────────────────────────────────────────
  obtenerTodasLasSolicitudes(): Observable<SolicitudDocenteResponse[]> {
    return this.http.get<SolicitudDocenteResponse[]>(`${this.apiUrl}/solicitudes-docente`);
  }

  obtenerSolicitudPorId(id: number): Observable<SolicitudDocenteResponse> {
    return this.http.get<SolicitudDocenteResponse>(`${this.apiUrl}/solicitudes-docente/${id}`);
  }

  obtenerSolicitudesPorEstado(estado: string): Observable<SolicitudDocenteResponse[]> {
    return this.http.get<SolicitudDocenteResponse[]>(
      `${this.apiUrl}/solicitudes-docente/estado/${estado}`
    );
  }

  obtenerSolicitudesDisponibles(): Observable<SolicitudDocenteResponse[]> {
    return this.http.get<SolicitudDocenteResponse[]>(
      `${this.apiUrl}/solicitudes-docente/disponibles-para-convocatoria`
    );
  }

  actualizarEstado(id: number, estado: string): Observable<SolicitudDocenteResponse> {
    return this.http.patch<SolicitudDocenteResponse>(
      `${this.apiUrl}/solicitudes-docente/${id}/estado`,
      null,
      { params: { estado } }
    );
  }

  eliminarSolicitud(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/solicitudes-docente/${id}`);
  }
}
