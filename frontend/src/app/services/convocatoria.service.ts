import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Convocatoria {
  idConvocatoria:        number;
  titulo:                string;
  descripcion:           string;
  fechaPublicacion:      string;
  fechaInicio:           string;
  fechaFin:              string;
  fechaLimiteDocumentos: string | null;  // NUEVO
  estadoConvocatoria:    string;
  documentosAbiertos:    boolean;        // NUEVO — true si hoy <= fechaLimiteDocumentos (o fechaFin)
}

export interface SolicitudDocente {
  idSolicitud:      number;
  idMateria:        number;
  nombreMateria:    string;
  idCarrera:        number;
  nombreCarrera:    string;
  idArea:           number;
  nombreArea:       string;
  justificacion:    string;
  cantidadDocentes: number;
  nivelAcademico:   string;
  estadoSolicitud:  string;
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

  obtenerSolicitudes(idConvocatoria: number): Observable<SolicitudDocente[]> {
    return this.http.get<SolicitudDocente[]>(`${this.apiUrl}/${idConvocatoria}/solicitudes`);
  }
}
