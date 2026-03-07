import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NivelAcademico {
  idNivel: number;
  nombre:  string;
  orden:   number;
  estado:  boolean;
}

@Injectable({ providedIn: 'root' })
export class NivelAcademicoService {

  private readonly BASE = '/api/niveles-academicos';

  constructor(private http: HttpClient) {}

  listar(): Observable<NivelAcademico[]> {
    return this.http.get<NivelAcademico[]>(this.BASE);
  }

  listarActivos(): Observable<NivelAcademico[]> {
    return this.http.get<NivelAcademico[]>(`${this.BASE}/activos`);
  }

  crear(data: Partial<NivelAcademico>): Observable<NivelAcademico> {
    return this.http.post<NivelAcademico>(this.BASE, data);
  }

  actualizar(id: number, data: Partial<NivelAcademico>): Observable<NivelAcademico> {
    return this.http.put<NivelAcademico>(`${this.BASE}/${id}`, data);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.BASE}/${id}`);
  }
}
