// services/matriz-meritos.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class MatrizMeritosService {

  private readonly API = 'http://localhost:8080/api/matriz-meritos';

  constructor(private http: HttpClient) {}

  obtenerMatriz(idConvocatoria: number): Observable<any> {
    return this.http.get<any>(`${this.API}/convocatoria/${idConvocatoria}`);
  }

  guardarPuntajes(payload: any): Observable<any> {
    return this.http.post<any>(`${this.API}/guardar`, payload);
  }
}
