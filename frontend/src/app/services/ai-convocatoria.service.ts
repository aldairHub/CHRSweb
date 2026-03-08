import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface GenerarDescripcionRequest {
  idsSolicitudes?: number[];
  justificaciones?: string[];
}

export interface GenerarDescripcionResponse {
  descripcion: string;
  error?: string;
}

@Injectable({ providedIn: 'root' })
export class AiConvocatoriaService {

  private readonly API = `${environment.apiUrl}/revisor/convocatorias/generar-descripcion`;

  constructor(private http: HttpClient) {}

  generarDescripcion(req: GenerarDescripcionRequest): Observable<GenerarDescripcionResponse> {
    return this.http.post<GenerarDescripcionResponse>(this.API, req);
  }
}
