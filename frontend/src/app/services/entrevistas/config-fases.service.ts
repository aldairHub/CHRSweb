// services/fases.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FaseRequest, FaseResponse } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class FasesService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/fases';

  constructor(private http: HttpClient) {}

  listar(): Observable<FaseResponse[]> {
    return this.http.get<FaseResponse[]>(this.apiUrl);
  }

  listarActivas(): Observable<FaseResponse[]> {
    return this.http.get<FaseResponse[]>(this.apiUrl, {
      params: { solo_activas: 'true' }
    });
  }

  obtener(id: number): Observable<FaseResponse> {
    return this.http.get<FaseResponse>(`${this.apiUrl}/${id}`);
  }

  crear(data: FaseRequest): Observable<FaseResponse> {
    return this.http.post<FaseResponse>(this.apiUrl, data);
  }

  actualizar(id: number, data: FaseRequest): Observable<FaseResponse> {
    return this.http.put<FaseResponse>(`${this.apiUrl}/${id}`, data);
  }

  cambiarEstado(id: number, estado: boolean): Observable<any> {
    return this.http.patch(`${this.apiUrl}/${id}/estado`, null, {
      params: { estado: String(estado) }
    });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
