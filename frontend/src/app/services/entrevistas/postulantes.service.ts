// services/postulantes.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PostulanteResumen,
  PostulanteDetalle,
  CrearProcesoRequest,
  DashboardStats
} from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class PostulantesService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/procesos';

  constructor(private http: HttpClient) {}

  listar(estado?: string, query?: string): Observable<PostulanteResumen[]> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    if (query)  params = params.set('query', query);
    return this.http.get<PostulanteResumen[]>(this.apiUrl, { params });
  }

  obtenerDetalle(idProceso: number): Observable<PostulanteDetalle> {
    return this.http.get<PostulanteDetalle>(`${this.apiUrl}/${idProceso}`);
  }

  crearProceso(data: CrearProcesoRequest): Observable<PostulanteResumen> {
    return this.http.post<PostulanteResumen>(this.apiUrl, data);
  }

  dashboard(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${this.apiUrl}/dashboard`);
  }
}
