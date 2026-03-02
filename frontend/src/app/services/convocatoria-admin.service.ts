import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConvocatoriaListaResponse {
  idConvocatoria: number;
  titulo: string;
  descripcion: string;
  fechaPublicacion: string;
  fechaInicio: string;
  fechaFin: string;
  estadoConvocatoria: string;
  totalSolicitudes: number;
}

export interface ConvocatoriaDetalleResponse {
  idConvocatoria: number;
  titulo: string;
  descripcion: string;
  fechaPublicacion: string;
  fechaInicio: string;
  fechaFin: string;
  estadoConvocatoria: string;
  solicitudes: SolicitudResumen[];
}

export interface SolicitudResumen {
  idSolicitud: number;
  nombreMateria: string;
  nombreCarrera: string;
  nombreFacultad: string;
  cantidadDocentes: number;
  nivelAcademico: string;
  estadoSolicitud: string;
}

export interface CrearConvocatoriaRequest {
  titulo: string;
  descripcion: string;
  fechaPublicacion: string;
  fechaInicio: string;
  fechaFin: string;
  idsSolicitudes: number[];
}

export interface ActualizarConvocatoriaRequest {
  titulo: string;
  descripcion: string;
  fechaPublicacion: string;
  fechaInicio: string;
  fechaFin: string;
}

export interface MensajeResponse {
  exito: boolean;
  mensaje: string;
  data: any;
}

@Injectable({ providedIn: 'root' })
export class ConvocatoriaAdminService {

  private apiUrl = 'http://localhost:8080/api/admin/convocatorias';

  constructor(private http: HttpClient) {}

  listar(estado?: string, titulo?: string): Observable<ConvocatoriaListaResponse[]> {
    let params = new HttpParams();
    if (estado) params = params.set('estado', estado);
    if (titulo) params = params.set('titulo', titulo);
    return this.http.get<ConvocatoriaListaResponse[]>(this.apiUrl, { params });
  }

  detalle(id: number): Observable<ConvocatoriaDetalleResponse> {
    return this.http.get<ConvocatoriaDetalleResponse>(`${this.apiUrl}/${id}`);
  }

  crear(req: CrearConvocatoriaRequest): Observable<MensajeResponse> {
    return this.http.post<MensajeResponse>(this.apiUrl, req);
  }

  actualizar(id: number, req: ActualizarConvocatoriaRequest): Observable<MensajeResponse> {
    return this.http.put<MensajeResponse>(`${this.apiUrl}/${id}`, req);
  }

  cambiarEstado(id: number, nuevoEstado: string): Observable<MensajeResponse> {
    return this.http.patch<MensajeResponse>(`${this.apiUrl}/${id}/estado`, { nuevoEstado });
  }

  eliminar(id: number): Observable<MensajeResponse> {
    return this.http.delete<MensajeResponse>(`${this.apiUrl}/${id}`);
  }
}
