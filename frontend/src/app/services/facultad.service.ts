import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Facultad {
  id: number;
  nombre: string;
  estado: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class FacultadService {

  private apiUrl = 'http://localhost:8080/api/facultades';

  constructor(private http: HttpClient) {}

  listar(): Observable<Facultad[]> {
    return this.http.get<Facultad[]>(this.apiUrl);
  }

  crear(data: Partial<Facultad>): Observable<Facultad> {
    return this.http.post<Facultad>(this.apiUrl, data);
  }

  actualizar(id: number, data: Partial<Facultad>): Observable<Facultad> {
    return this.http.put<Facultad>(`${this.apiUrl}/${id}`, data);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
