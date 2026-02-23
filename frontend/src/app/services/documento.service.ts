import { Injectable } from '@angular/core';
import { HttpClient, HttpRequest, HttpEventType, HttpResponse } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

// ============================================================
// Interfaces que reflejan los DTOs del backend
// ============================================================
export interface DocumentoBackend {
  idTipoDocumento:  number;
  nombreTipo:       string;
  obligatorio:      boolean;
  idDocumento:      number | null;
  estadoValidacion: 'pendiente' | 'validado' | 'rechazado' | null;
  descripcionTipo:  string;
  rutaArchivo:      string | null;
  fechaCarga:       string | null;
  observacionesIa:  string | null;
}

export interface PostulanteInfo {
  idPostulante:      number;
  nombres:           string;
  apellidos:         string;
  identificacion:    string;
  correo:            string;
  idPostulacion:     number;
  estadoPostulacion: string;
  nombreMateria:     string;
  nombreCarrera:     string;
  nombreArea:        string;
}

export interface SubirDocumentoResponse {
  exitoso:     boolean;
  mensaje:     string;
  idDocumento: number | null;
  rutaArchivo: string | null;
}

export interface OperacionResponse {
  exitoso:  boolean;
  mensaje:  string;
  descripcionTipo:  string;
}

// ============================================================
// DocumentoService
// ============================================================
@Injectable({
  providedIn: 'root'
})
export class DocumentoService {

  private readonly API = `${environment.apiUrl}/documentos`;

  constructor(private http: HttpClient) {}

  // ----------------------------------------------------------
  // GET /api/documentos/postulante/{idUsuario}
  // Carga info del postulante y su postulación activa
  // ----------------------------------------------------------
  obtenerInfoPostulante(idUsuario: number): Observable<PostulanteInfo> {
    return this.http.get<PostulanteInfo>(`${this.API}/postulante/${idUsuario}`);
  }

  // ----------------------------------------------------------
  // GET /api/documentos/postulacion/{idPostulacion}
  // Lista tipos de doc + estado actual para una postulación
  // ----------------------------------------------------------
  obtenerDocumentos(idPostulacion: number): Observable<DocumentoBackend[]> {
    return this.http.get<DocumentoBackend[]>(`${this.API}/postulacion/${idPostulacion}`);
  }

  // ----------------------------------------------------------
  // POST /api/documentos/subir  (multipart/form-data)
  // Devuelve Observable<number> con el progreso (0-100)
  // y emite el resultado final como SubirDocumentoResponse
  // ----------------------------------------------------------
  subirDocumento(
    idPostulacion:   number,
    idTipoDocumento: number,
    archivo:         File,
    progreso$:       Subject<number>
  ): Observable<SubirDocumentoResponse> {
    const formData = new FormData();
    formData.append('idPostulacion',   idPostulacion.toString());
    formData.append('idTipoDocumento', idTipoDocumento.toString());
    formData.append('archivo',         archivo, archivo.name);

    const req = new HttpRequest('POST', `${this.API}/subir`, formData, {
      reportProgress: true
    });

    return new Observable(observer => {
      this.http.request(req).subscribe({
        next: event => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            const pct = Math.round(100 * event.loaded / event.total);
            progreso$.next(pct);
          } else if (event instanceof HttpResponse) {
            observer.next(event.body as SubirDocumentoResponse);
            observer.complete();
          }
        },
        error: err => {
          observer.error(err);
        }
      });
    });
  }

  // ----------------------------------------------------------
  // DELETE /api/documentos/{idDocumento}/postulacion/{idPostulacion}
  // ----------------------------------------------------------
  eliminarDocumento(idDocumento: number, idPostulacion: number): Observable<OperacionResponse> {
    return this.http.delete<OperacionResponse>(
      `${this.API}/${idDocumento}/postulacion/${idPostulacion}`
    );
  }

  // ----------------------------------------------------------
  // POST /api/documentos/finalizar/{idPostulacion}
  // ----------------------------------------------------------
  finalizarCarga(idPostulacion: number): Observable<OperacionResponse> {
    return this.http.post<OperacionResponse>(
      `${this.API}/finalizar/${idPostulacion}`, {}
    );
  }
}
