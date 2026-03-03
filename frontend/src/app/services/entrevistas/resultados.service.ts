// services/resultados.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ResultadoProceso, DecisionFinalRequest } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class ResultadosService {

  private base = 'http://localhost:8080/api/evaluacion';

  constructor(private http: HttpClient) {}

  /** GET /api/evaluacion/procesos/{idProceso}/resultados */
  obtenerResultados(idProceso: number): Observable<ResultadoProceso> {
    return this.http.get<ResultadoProceso>(`${this.base}/procesos/${idProceso}/resultados`);
  }

  /** POST /api/evaluacion/procesos/{idProceso}/decision */
  guardarDecision(idProceso: number, data: DecisionFinalRequest): Observable<ResultadoProceso> {
    return this.http.post<ResultadoProceso>(`${this.base}/procesos/${idProceso}/decision`, data);
  }

  /** Abre el reporte PDF en una nueva pestaña */
  abrirPDF(idProceso: number): void {
    window.open(`${this.base}/procesos/${idProceso}/resultados/pdf`, '_blank');
  }
}
