// services/config-criterios.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CriterioRequest, CriterioResponse, PesoTotalResponse } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class ConfigCriteriosService {

  private base = 'http://localhost:8080/api/evaluacion';

  constructor(private http: HttpClient) {}

  /** GET /api/evaluacion/plantillas/{idPlantilla}/criterios */
  listarPorPlantilla(idPlantilla: number): Observable<CriterioResponse[]> {
    return this.http.get<CriterioResponse[]>(`${this.base}/plantillas/${idPlantilla}/criterios`);
  }

  /** GET /api/evaluacion/criterios/por-fase/{idFase} */
  listarPorFase(idFase: number): Observable<CriterioResponse[]> {
    return this.http.get<CriterioResponse[]>(`${this.base}/criterios/por-fase/${idFase}`);
  }

  /** GET /api/evaluacion/criterios/{id} */
  obtener(id: number): Observable<CriterioResponse> {
    return this.http.get<CriterioResponse>(`${this.base}/criterios/${id}`);
  }

  pesoTotal(idPlantilla: number): Observable<PesoTotalResponse> {
    return this.http.get<PesoTotalResponse>(`${this.base}/criterios/peso-total`, {
      params: { idPlantilla: String(idPlantilla) }
    });
  }

  /** POST /api/evaluacion/criterios */
  crear(data: CriterioRequest): Observable<CriterioResponse> {
    return this.http.post<CriterioResponse>(`${this.base}/criterios`, data);
  }

  /** PUT /api/evaluacion/criterios/{id} */
  actualizar(id: number, data: CriterioRequest): Observable<CriterioResponse> {
    return this.http.put<CriterioResponse>(`${this.base}/criterios/${id}`, data);
  }

  /** DELETE /api/evaluacion/criterios/{id} */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/criterios/${id}`);
  }
}
