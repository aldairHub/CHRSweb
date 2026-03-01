// services/programar-reunion.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ReunionRequest, ReunionResumen } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class ProgramarReunionService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/reuniones';

  constructor(private http: HttpClient) {}

  programar(data: ReunionRequest): Observable<ReunionResumen> {
    return this.http.post<ReunionResumen>(this.apiUrl, data);
  }

  reprogramar(id: number, data: ReunionRequest): Observable<ReunionResumen> {
    return this.http.put<ReunionResumen>(`${this.apiUrl}/${id}`, data);
  }

  cancelar(id: number): Observable<ReunionResumen> {
    return this.http.patch<ReunionResumen>(`${this.apiUrl}/${id}/estado`, null, {
      params: { estado: 'cancelada' }
    });
  }
}
