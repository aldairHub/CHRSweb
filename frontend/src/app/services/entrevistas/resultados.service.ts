// services/resultados.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ResultadoProceso, DecisionFinalRequest } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class ResultadosService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/evaluaciones';

  constructor(private http: HttpClient) {}

  obtenerResultados(idProceso: number): Observable<ResultadoProceso> {
    return this.http.get<ResultadoProceso>(`${this.apiUrl}/resultados/${idProceso}`);
  }

  guardarDecision(idProceso: number, data: DecisionFinalRequest): Observable<ResultadoProceso> {
    return this.http.post<ResultadoProceso>(`${this.apiUrl}/decision/${idProceso}`, data);
  }

  /**
   * Abre el reporte PDF en una nueva pestaña.
   * Backend: GET /api/evaluacion/evaluaciones/resultados/{idProceso}/pdf
   */
  abrirPDF(idProceso: number): void {
    window.open(`${this.apiUrl}/resultados/${idProceso}/pdf`, '_blank');
  }
}
