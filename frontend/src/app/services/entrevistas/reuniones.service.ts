// services/reuniones.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReunionRequest, ReunionResumen } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class ReunionesService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/reuniones';

  constructor(private http: HttpClient) {}

  programar(data: ReunionRequest): Observable<ReunionResumen> {
    return this.http.post<ReunionResumen>(this.apiUrl, data);
  }

  obtener(id: number): Observable<ReunionResumen> {
    return this.http.get<ReunionResumen>(`${this.apiUrl}/${id}`);
  }

  /**
   * Lista reunion con estado=programada para el dashboard y próximas 7 días.
   * Backend: GET /api/evaluacion/reuniones? estado =programada
   */
  listarProgramadas(): Observable<ReunionResumen[]> {
    return this.http.get<ReunionResumen[]>(this.apiUrl, {
      params: { estado: 'programada' }
    });
  }

  cambiarEstado(id: number, estado: 'programada' | 'en_curso' | 'completada' | 'cancelada'): Observable<ReunionResumen> {
    return this.http.patch<ReunionResumen>(`${this.apiUrl}/${id}/estado`, null, {
      params: { estado }
    });
  }
}
