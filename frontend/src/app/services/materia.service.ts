import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Materia {
  id?: number;
  nombre: string;
  nivel: number;
  idCarrera: number;
}

@Injectable({
  providedIn: 'root'
})
export class MateriaService {

  private apiUrl = 'http://localhost:8080/api/materias';

  constructor(private http: HttpClient) {}

  listar(): Observable<Materia[]> {
    return this.http.get<Materia[]>(this.apiUrl);
  }

  crear(materia: Materia): Observable<Materia> {
    return this.http.post<Materia>(this.apiUrl, materia);
  }

  actualizar(id: number, materia: Materia): Observable<Materia> {
    return this.http.put<Materia>(`${this.apiUrl}/${id}`, materia);
  }
}
