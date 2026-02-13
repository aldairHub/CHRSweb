import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

// Modelos de datos
export interface Prepostulacion {
  idPrepostulacion: number;
  identificacion: string;
  nombres: string;
  apellidos: string;
  correo: string;
  estadoRevision: string;
  fechaEnvio: string;
  fechaRevision?: string;
  observacionesRevision?: string;
  urlCedula?: string;
  urlFoto?: string;
  urlPrerrequisitos?: string;
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
  idRevisor?: number;
}

@Injectable({
  providedIn: 'root'
})
export class PrepostulacionService {

  // ⚠️ IMPORTANTE: Cambia esta URL por la de tu backend
  private apiUrl = 'http://localhost:8080/api/admin/prepostulaciones';

  constructor(private http: HttpClient) { }

  /**
   * Listar todas las prepostulaciones
   * GET /api/admin/prepostulaciones
   */
  listarPrepostulaciones(): Observable<Prepostulacion[]> {
    return this.http.get<Prepostulacion[]>(this.apiUrl);
  }

  /**
   * Listar prepostulaciones por estado
   * GET /api/admin/prepostulaciones/estado/{estado}
   */
  listarPorEstado(estado: string): Observable<Prepostulacion[]> {
    return this.http.get<Prepostulacion[]>(`${this.apiUrl}/estado/${estado}`);
  }

  /**
   * Obtener una prepostulación específica
   * GET /api/admin/prepostulaciones/{id}
   */
  obtenerPrepostulacion(id: number): Observable<Prepostulacion> {
    return this.http.get<Prepostulacion>(`${this.apiUrl}/${id}`);
  }

  /**
   * Obtener documentos de una prepostulación
   * GET /api/admin/prepostulaciones/{id}/documentos
   */
  obtenerDocumentos(id: number): Observable<DocumentosResponse> {
    return this.http.get<DocumentosResponse>(`${this.apiUrl}/${id}/documentos`);
  }

  /**
   * Actualizar estado de una prepostulación
   * PUT /api/admin/prepostulaciones/{id}/estado
   */
  actualizarEstado(id: number, datos: ActualizarEstadoRequest): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/estado`, datos);
  }
}
