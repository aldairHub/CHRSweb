import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';
import { throwError } from 'rxjs';

// ==========================================
// INTERFACES EXISTENTES
// ==========================================
export interface Prepostulacion {
  idPrepostulacion: number;
  nombres: string;
  apellidos: string;
  identificacion: string;
  correo: string;
  estadoRevision: string;
  fechaEnvio: string;
  urlCedula?: string;
  urlFoto?: string;
  urlPrerrequisitos?: string;
  observacionesRevision?: string;
  fechaRevision?: string;
  idRevisor?: number;
}

export interface DocumentoAcademico {
  idDocumento?: number;
  descripcion: string;
  urlDocumento: string;
  fechaSubida?: string;
}

export interface DocumentosResponse {
  cedula: string;
  foto: string;
  documentosAcademicos: DocumentoAcademico[];
  nombreCompleto: string;
  identificacion: string;
}

export interface DocumentoEntrada {
  archivo: File | null;
  descripcion: string;
  nombreArchivo: string;
}

export interface ActualizarEstadoRequest {
  estado: string;
  observaciones: string;
  idRevisor: number;
}

// ==========================================
// INTERFACES NUEVAS
// ==========================================
export interface Convocatoria {
  idConvocatoria: number;
  titulo: string;
  descripcion: string;
  estadoConvocatoria: string;
  fechaInicio: string;
  fechaFin: string;
  fechaPublicacion: string;
  imagenPortadaUrl?: string;
}

export interface SolicitudDocente {
  idSolicitud: number;
  nivelAcademico: string;
  experienciaDocenteMin: number;
  experienciaProfesionalMin: number;
  cantidadDocentes: number;
  justificacion: string;
  estadoSolicitud: string;
  observaciones?: string;
  fechaSolicitud: string;
  idMateria: number;
  idCarrera: number;
  idArea: number;
}

export interface DetallePostulacion {
  solicitud: SolicitudDocente;
  convocatoria: Convocatoria;
}

// ==========================================
// SERVICIO
// ==========================================
@Injectable({
  providedIn: 'root'
})
export class PrepostulacionService {

  private apiUrl         = 'http://localhost:8080/api/admin/prepostulaciones';
  private convocatoriaUrl = 'http://localhost:8080/api/admin/convocatorias';
  private solicitudUrl    = 'http://localhost:8080/api/solicitudes-docente';

  constructor(private http: HttpClient) {}

  listarPrepostulaciones(): Observable<Prepostulacion[]> {
    return this.http.get<Prepostulacion[]>(this.apiUrl).pipe(
      tap(data => console.log('✅ Prepostulaciones recibidas:', data)),
      catchError(err => { console.error('❌ Error:', err); return throwError(() => err); })
    );
  }

  obtenerPrepostulacion(id: number): Observable<Prepostulacion> {
    return this.http.get<Prepostulacion>(`${this.apiUrl}/${id}`).pipe(
      tap(data => console.log('✅ Prepostulación:', data)),
      catchError(err => { console.error('❌ Error:', err); return throwError(() => err); })
    );
  }

  obtenerDocumentos(id: number): Observable<DocumentosResponse> {
    return this.http.get<DocumentosResponse>(`${this.apiUrl}/${id}/documentos`).pipe(
      tap(data => console.log('✅ Documentos:', data)),
      catchError(err => { console.error('❌ Error:', err); return throwError(() => err); })
    );
  }

  actualizarEstado(id: number, request: ActualizarEstadoRequest): Observable<any> {
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });
    return this.http.put(`${this.apiUrl}/${id}/estado`, request, { headers }).pipe(
      tap(response => console.log('✅ Estado actualizado:', response)),
      catchError(err => { console.error('❌ Error:', err); return throwError(() => err); })
    );
  }

  listarPorEstado(estado: string): Observable<Prepostulacion[]> {
    return this.http.get<Prepostulacion[]>(`${this.apiUrl}/estado/${estado}`).pipe(
      tap(data => console.log('✅ Por estado:', data)),
      catchError(err => { console.error('❌ Error:', err); return throwError(() => err); })
    );
  }

  // ── Métodos nuevos ────────────────────────────────────────

  /**
   * GET /api/admin/prepostulaciones/{id}/detalle
   */
  obtenerDetalle(idPrepostulacion: number): Observable<DetallePostulacion> {
    return this.http.get<DetallePostulacion>(`${this.apiUrl}/${idPrepostulacion}/detalle`).pipe(
      tap(data => console.log('✅ Detalle:', data)),
      catchError(err => { console.error('❌ Error detalle:', err); return throwError(() => err); })
    );
  }

  /**
   * GET /api/convocatorias — devuelve [] si falla
   */
  listarConvocatorias(): Observable<Convocatoria[]> {
    return this.http.get<Convocatoria[]>(this.convocatoriaUrl).pipe(
      tap(data => console.log('✅ Convocatorias:', data)),
      catchError(err => {
        console.warn('⚠️ Convocatorias no disponibles:', err.status);
        return of([]);
      })
    );
  }

  /**
   * GET /api/solicitudes-docente — devuelve [] si falla
   */
  listarSolicitudes(): Observable<SolicitudDocente[]> {
    return this.http.get<SolicitudDocente[]>(this.solicitudUrl).pipe(
      tap(data => console.log('✅ Solicitudes:', data)),
      catchError(err => {
        console.warn('⚠️ Solicitudes no disponibles:', err.status);
        return of([]);
      })
    );
  }
}
