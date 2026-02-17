import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface KpiDto {
  titulo: string;
  valor: number;
  color: string;
  icono: string;
}

export interface ReporteRecienteDto {
  nombre: string;
  fecha: string;
  peso: string;
  tipo: string;
}

export interface RendimientoAreaDto {
  area: string;
  puntaje: number;
  color: string;
}

@Injectable({
  providedIn: 'root'
})
export class ReportesService {

  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // ================= KPIs =================
  obtenerKpis(): Observable<KpiDto[]> {
    return this.http.get<KpiDto[]>(`${this.apiUrl}/reportes/kpis`);
  }

  // ================= REPORTES =================
  obtenerReportesRecientes(): Observable<ReporteRecienteDto[]> {
    return this.http.get<ReporteRecienteDto[]>(`${this.apiUrl}/reportes/recientes`);
  }

  // ================= RENDIMIENTO =================
  obtenerRendimientoArea(): Observable<RendimientoAreaDto[]> {
    return this.http.get<RendimientoAreaDto[]>(`${this.apiUrl}/reportes/rendimiento-area`);
  }

  // ================= VER =================
  verReporte(nombre: string) {
    window.open(`${this.apiUrl}/reportes/ver/${nombre}`, '_blank');
  }

  // ================= DESCARGAR =================
  descargarReporte(nombre: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/reportes/descargar/${nombre}`, {
      responseType: 'blob'
    });
  }
}
