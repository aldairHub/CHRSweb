import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface EntrevistaInfo {
  idReunion:        number;
  nombreFase:       string;
  fecha:            string;
  hora:             string;
  duracion:         number;
  modalidad:        string;
  enlace:           string | null;
  evaluadores:      string[];
  observaciones:    string | null;
  estado:           'programada' | 'en_curso' | 'completada' | 'cancelada';
}

@Injectable({ providedIn: 'root' })
export class EntrevistaService {
  private readonly API = `${environment.apiUrl}/evaluacion/reuniones`;

  constructor(private http: HttpClient) {}

  obtenerMiEntrevista(idUsuario: number): Observable<EntrevistaInfo> {
    return this.http.get<EntrevistaInfo>(`${this.API}/mi-entrevista`, {
      params: { idUsuario: idUsuario.toString() }
    });
  }
}
