import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Carrera } from '../models/carrera.model';

@Injectable({
  providedIn: 'root'
})
export class CarreraService {

  private apiUrl = 'http://localhost:8080/api/carreras';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Carrera[]> {
    return this.http.get<Carrera[]>(this.apiUrl);
  }

  create(carrera: {
    id: number;
    idFacultad: number | null;
    nombreCarrera: string;
    modalidad: string;
    estado: boolean
  }): Observable<Carrera> {
    return this.http.post<Carrera>(this.apiUrl, carrera);
  }
  update(id: number, carrera: {
    id: number;
    idFacultad: number | null;
    nombreCarrera: string;
    modalidad: string;
    estado: boolean
  }): Observable<Carrera> {
    return this.http.put<Carrera>(`${this.apiUrl}/${id}`, carrera);
  }

  toggleEstado(id: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/estado`, {});
  }
}
