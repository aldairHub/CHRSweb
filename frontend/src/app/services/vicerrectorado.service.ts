// src/app/services/vicerrectorado.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PrepostulacionResumen {
  idPrepostulacion: number;
  identificacion: string;
  nombreCompleto: string;
  correo: string;
  estadoRevision: string;  // 'PENDIENTE' | 'APROBADO' | 'RECHAZADO'
  fechaEnvio: string;
  documentosCompletos: boolean;
}

export interface EstadisticasPrepostulacion {
  total: number;
  pendientes: number;
  aprobadas: number;
  rechazadas: number;
}

export interface ActualizarEstadoRequest {
  estado: string;
  observaciones: string;
  idRevisor: number;
}

export interface SolicitudDocente {
  idSolicitud?: number;
  [key: string]: any;
}

@Injectable({ providedIn: 'root' })
export class VicerrectoradoService {

  private base = '/api/vicerrectorado';
  private solicitudesBase = '/api/solicitudes-docente';

  constructor(private http: HttpClient) {}

  // ─── Prepostulaciones ────────────────────────────────────────

  listarResumen(): Observable<PrepostulacionResumen[]> {
    return this.http.get<PrepostulacionResumen[]>(`${this.base}/prepostulaciones/resumen`);
  }

  listarPorEstado(estado: string): Observable<PrepostulacionResumen[]> {
    return this.http.get<PrepostulacionResumen[]>(`${this.base}/prepostulaciones/estado/${estado}`);
  }

  obtenerEstadisticas(): Observable<EstadisticasPrepostulacion> {
    return this.http.get<EstadisticasPrepostulacion>(`${this.base}/prepostulaciones/estadisticas`);
  }

  actualizarEstado(id: number, req: ActualizarEstadoRequest): Observable<any> {
    return this.http.put(`${this.base}/prepostulaciones/${id}/estado`, req);
  }

  obtenerDocumentos(id: number): Observable<any> {
    return this.http.get(`${this.base}/prepostulaciones/${id}/documentos`);
  }

  buscar(query: string): Observable<PrepostulacionResumen[]> {
    return this.http.get<PrepostulacionResumen[]>(`${this.base}/prepostulaciones/buscar`, {
      params: { query }
    });
  }

  // ─── Solicitudes de docente ──────────────────────────────────

  listarSolicitudes(): Observable<SolicitudDocente[]> {
    return this.http.get<SolicitudDocente[]>(this.solicitudesBase);
  }
}
