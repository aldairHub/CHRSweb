import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface EntrevistaInfo {
  idReunion:     number;
  nombreFase:    string;
  fecha:         string;
  hora:          string;
  duracion:      number;
  modalidad:     string;
  enlace:        string | null;
  evaluadores:   string[];
  observaciones: string | null;
  estado:        'programada' | 'en_curso' | 'completada' | 'cancelada';
}

@Injectable({ providedIn: 'root' })
export class EntrevistaService {
  private readonly API = `${environment.apiUrl}/evaluacion/reuniones`;

  constructor(private http: HttpClient) {}

  /**
   * Retorna la entrevista del postulante o null si no tiene ninguna.
   * Nunca lanza error — convierte cualquier fallo HTTP en null
   * para que el componente muestre el estado vacío sin redirigir.
   */
  obtenerMiEntrevista(idUsuario: number, idPostulacion?: number): Observable<EntrevistaInfo | null> {
    const params: any = { idUsuario: idUsuario.toString() };
    if (idPostulacion) params['idPostulacion'] = idPostulacion.toString();
    return this.http
      .get<EntrevistaInfo | null>(`${this.API}/mi-entrevista`, { params })
      .pipe(catchError(() => of(null)));
  }
}
