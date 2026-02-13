import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, tap, catchError } from 'rxjs';
import { throwError } from 'rxjs';

// Interfaces que coinciden EXACTAMENTE con tu backend
export interface Prepostulacion {
  idPrepostulacion: number;
  nombres: string;
  apellidos: string;
  identificacion: string;
  correo: string;
  estadoRevision: string; // "PENDIENTE", "APROBADO", "RECHAZADO"
  fechaEnvio: string;
  urlCedula?: string;
  urlFoto?: string;
  urlPrerrequisitos?: string;
  observacionesRevision?: string;
  fechaRevision?: string;
  idRevisor?: number;
}

export interface DocumentosResponse {
  cedula: string;
  foto: string;
  prerrequisitos: string;
  nombreCompleto: string;
  identificacion: string;
}

export interface ActualizarEstadoRequest {
  estado: string;
  observaciones: string;
  idRevisor: number;
}

@Injectable({
  providedIn: 'root'
})
export class PrepostulacionService {

  private apiUrl = 'http://localhost:8080/api/admin/prepostulaciones';

  constructor(private http: HttpClient) {}

  /**
   * Lista todas las prepostulaciones
   */
  listarPrepostulaciones(): Observable<Prepostulacion[]> {
    console.log('🔄 Llamando a:', `${this.apiUrl}`);

    return this.http.get<Prepostulacion[]>(this.apiUrl).pipe(
      tap(data => console.log('✅ Prepostulaciones recibidas:', data)),
      catchError(err => {
        console.error('❌ Error al listar prepostulaciones:', err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Obtiene una prepostulación específica por ID
   */
  obtenerPrepostulacion(id: number): Observable<Prepostulacion> {
    console.log('🔄 Obteniendo prepostulación ID:', id);

    return this.http.get<Prepostulacion>(`${this.apiUrl}/${id}`).pipe(
      tap(data => console.log('✅ Prepostulación recibida:', data)),
      catchError(err => {
        console.error('❌ Error al obtener prepostulación:', err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Obtiene las URLs de los documentos de una prepostulación
   */
  obtenerDocumentos(id: number): Observable<DocumentosResponse> {
    console.log('🔄 Obteniendo documentos para ID:', id);

    return this.http.get<DocumentosResponse>(`${this.apiUrl}/${id}/documentos`).pipe(
      tap(data => console.log('✅ Documentos recibidos:', data)),
      catchError(err => {
        console.error('❌ Error al obtener documentos:', err);
        return throwError(() => err);
      })
    );
  }

  /**
   * Actualiza el estado de revisión de una prepostulación
   */
  actualizarEstado(id: number, request: ActualizarEstadoRequest): Observable<any> {
    console.log('🔄 Actualizando estado para ID:', id);
    console.log('📤 Request body:', request);

    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    return this.http.put(`${this.apiUrl}/${id}/estado`, request, { headers }).pipe(
      tap(response => console.log('✅ Estado actualizado:', response)),
      catchError(err => {
        console.error('❌ Error al actualizar estado:', err);
        console.error('📋 Detalles del error:', {
          status: err.status,
          message: err.message,
          error: err.error
        });
        return throwError(() => err);
      })
    );
  }

  /**
   * Lista prepostulaciones por estado
   */
  listarPorEstado(estado: string): Observable<Prepostulacion[]> {
    console.log('🔄 Listando por estado:', estado);

    return this.http.get<Prepostulacion[]>(`${this.apiUrl}/estado/${estado}`).pipe(
      tap(data => console.log('✅ Prepostulaciones por estado:', data)),
      catchError(err => {
        console.error('❌ Error al listar por estado:', err);
        return throwError(() => err);
      })
    );
  }
}
