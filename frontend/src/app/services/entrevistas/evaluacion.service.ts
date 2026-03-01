// services/evaluacion.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EvaluacionRequest,
  EvaluacionResponse,
  ResultadoProceso,
  DecisionFinalRequest
} from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class EvaluacionService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/evaluaciones';

  constructor(private http: HttpClient) {}

  guardar(data: EvaluacionRequest): Observable<EvaluacionResponse> {
    return this.http.post<EvaluacionResponse>(this.apiUrl, data);
  }

  obtener(id: number): Observable<EvaluacionResponse> {
    return this.http.get<EvaluacionResponse>(`${this.apiUrl}/${id}`);
  }

  listarPorReunion(idReunion: number): Observable<EvaluacionResponse[]> {
    return this.http.get<EvaluacionResponse[]>(this.apiUrl, {
      params: { idReunion: String(idReunion) }
    });
  }

  obtenerResultados(idProceso: number): Observable<ResultadoProceso> {
    return this.http.get<ResultadoProceso>(`${this.apiUrl}/resultados/${idProceso}`);
  }

  guardarDecision(idProceso: number, data: DecisionFinalRequest): Observable<ResultadoProceso> {
    return this.http.post<ResultadoProceso>(`${this.apiUrl}/decision/${idProceso}`, data);
  }
}
