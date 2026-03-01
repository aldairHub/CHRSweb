// services/config-criterios.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CriterioRequest, CriterioResponse, PesoTotalResponse } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class ConfigCriteriosService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/criterios';

  constructor(private http: HttpClient) {}

  listarPorPlantilla(idPlantilla: number): Observable<CriterioResponse[]> {
    return this.http.get<CriterioResponse[]>(this.apiUrl, {
      params: { idPlantilla: String(idPlantilla) }
    });
  }

  /**
   * Carga criterios de la plantilla activa asociada a una fase.
   * Usado en el componente de evaluación para cargar los criterios al abrir una reunión.
   * Backend: GET /api/evaluacion/criterios?idFase=X
   */
  listarPorFase(idFase: number): Observable<CriterioResponse[]> {
    return this.http.get<CriterioResponse[]>(this.apiUrl, {
      params: { idFase: String(idFase) }
    });
  }

  obtener(id: number): Observable<CriterioResponse> {
    return this.http.get<CriterioResponse>(`${this.apiUrl}/${id}`);
  }

  pesoTotal(idPlantilla: number): Observable<PesoTotalResponse> {
    return this.http.get<PesoTotalResponse>(`${this.apiUrl}/peso-total`, {
      params: { idPlantilla: String(idPlantilla) }
    });
  }

  crear(data: CriterioRequest): Observable<CriterioResponse> {
    return this.http.post<CriterioResponse>(this.apiUrl, data);
  }

  actualizar(id: number, data: CriterioRequest): Observable<CriterioResponse> {
    return this.http.put<CriterioResponse>(`${this.apiUrl}/${id}`, data);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
