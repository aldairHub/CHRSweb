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
  facultad?: {
    idFacultad: number;
    nombreFacultad: string;
  };
}

export interface Materia {
  idMateria: number;
  nombre: string;
  nivel: number;
  carrera?: {
    idCarrera: number;
    nombreCarrera: string;
  };
}

export interface AreaConocimiento {
  idArea: number;
  nombreArea: string;
}

// ⭐ NUEVO: Interface simple con solo usuario_app
export interface SolicitudConUsuario {
  usuarioApp: string;
  solicitud: SolicitudDocenteRequest;
}

// ⚠️ LEGACY: Mantener por compatibilidad (no se usa)
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

  // ========== CATÁLOGOS ==========

  obtenerCarreras(): Observable<Carrera[]> {
    return this.http.get<Carrera[]>(`${this.apiUrl}/carreras`);
  }

  obtenerMateriasPorCarrera(idCarrera: number): Observable<Materia[]> {
    return this.http.get<Materia[]>(`${this.apiUrl}/materias/carrera/${idCarrera}`);
  }

  obtenerAreasConocimiento(): Observable<AreaConocimiento[]> {
    return this.http.get<AreaConocimiento[]>(`${this.apiUrl}/areas-conocimiento`);
  }

  // ========== SOLICITUDES - MÉTODO ACTUALIZADO ==========

  /**
   * ⭐ NUEVO: Método simplificado que solo usa usuario_app
   */
  crearSolicitud(request: SolicitudConUsuario): Observable<SolicitudDocenteResponse> {
    console.log('📤 Servicio enviando solicitud:', request);

    return this.http.post<SolicitudDocenteResponse>(
      `${this.apiUrl}/solicitudes-docente`,
      request
    );
  }

  /**
   * ⚠️ LEGACY: Método antiguo mantenido por compatibilidad
   * Ahora solo convierte y llama al nuevo método
   */
  crearSolicitudConCredenciales(request: SolicitudConCredenciales): Observable<SolicitudDocenteResponse> {
    // Convertir al nuevo formato (ignora credenciales, usa solo usuarioBd como usuarioApp)
    const nuevoRequest: SolicitudConUsuario = {
      usuarioApp: request.usuarioBd,
      solicitud: request.solicitud
    };

    return this.crearSolicitud(nuevoRequest);
  }

  // ========== OTROS MÉTODOS ==========

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

  obtenerSolicitudesPorAutoridad(idAutoridad: number): Observable<SolicitudDocenteResponse[]> {
    return this.http.get<SolicitudDocenteResponse[]>(
      `${this.apiUrl}/solicitudes-docente/autoridad/${idAutoridad}`
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
