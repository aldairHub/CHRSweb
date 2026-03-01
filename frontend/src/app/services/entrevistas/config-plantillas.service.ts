// services/config-plantillas.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PlantillaRequest, PlantillaResponse } from '../../models/entrevistas-models';

@Injectable({
  providedIn: 'root'
})
export class ConfigPlantillasService {

  private apiUrl = 'http://localhost:8080/api/evaluacion/plantillas';

  constructor(private http: HttpClient) {}

  listar(idFase?: number): Observable<PlantillaResponse[]> {
    let params = new HttpParams();
    if (idFase != null) params = params.set('idFase', idFase);
    return this.http.get<PlantillaResponse[]>(this.apiUrl, { params });
  }

  obtener(id: number): Observable<PlantillaResponse> {
    return this.http.get<PlantillaResponse>(`${this.apiUrl}/${id}`);
  }

  crear(data: PlantillaRequest): Observable<PlantillaResponse> {
    return this.http.post<PlantillaResponse>(this.apiUrl, data);
  }

  actualizar(id: number, data: PlantillaRequest): Observable<PlantillaResponse> {
    return this.http.put<PlantillaResponse>(`${this.apiUrl}/${id}`, data);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
