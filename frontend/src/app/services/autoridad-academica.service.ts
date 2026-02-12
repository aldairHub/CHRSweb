import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AutoridadAcademicaService {

  // 🔧 AJUSTA el puerto / contexto a tu backend Spring
  private readonly API = 'http://localhost:8080/api/autoridades';

  constructor(private http: HttpClient) {}

  // =========================
  // AUTORIDAD ACADEMICA
  // =========================

  listarAutoridades(): Observable<any[]> {
    return this.http.get<any[]>(this.API);
  }

  registrarAutoridad(payload: any): Observable<any> {
    return this.http.post<any>(this.API, payload);
  }

  cambiarEstadoAutoridad(idAutoridad: number, estado: boolean): Observable<void> {
    return this.http.put<void>(
      `${this.API}/${idAutoridad}/estado`,
      { estado }
    );
  }

  // =========================
  // CATALOGOS
  // =========================

  listarInstituciones(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/instituciones`);
  }

  listarCargosAutoridad(): Observable<any[]> {
    return this.http.get<any[]>(`${this.API}/roles`);
  }
}
