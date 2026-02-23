import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Convocatoria {
  idConvocatoria:      number;
  titulo:              string;
  descripcion:         string;
  fechaPublicacion:    string;
  fechaInicio:         string;
  fechaFin:            string;
  estadoConvocatoria:  string; // 'abierta' | 'cerrada' | 'cancelada'
}

@Injectable({ providedIn: 'root' })
export class ConvocatoriaService {

  private apiUrl = 'http://localhost:8080/api/convocatorias';

  constructor(private http: HttpClient) {}

  listarAbiertas(): Observable<Convocatoria[]> {
    return this.http.get<Convocatoria[]>(`${this.apiUrl}/activas`);
  }

  obtener(id: number): Observable<Convocatoria> {
    return this.http.get<Convocatoria>(`${this.apiUrl}/${id}`);
  }
}
