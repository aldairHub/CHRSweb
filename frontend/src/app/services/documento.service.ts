import { Injectable } from '@angular/core';
import { HttpClient, HttpRequest, HttpEventType, HttpResponse } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { environment } from '../../environments/environment';

// ============================================================
// Interfaces
// ============================================================
export interface DocumentoBackend {
  idTipoDocumento:  number;
  nombreTipo:       string;
  obligatorio:      boolean;
  idDocumento:      number | null;
  estadoValidacion: string | null;
  descripcionTipo:  string | null;
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
  documentosAbiertos:    boolean;
  fechaLimiteDocumentos: string;
  // NUEVO: para el selector de convocatoria
  idConvocatoria?:      number;
  nombreConvocatoria?:  string;
}

export interface SubirDocumentoResponse {
  exitoso:     boolean;
  mensaje:     string;
  idDocumento: number | null;
  rutaArchivo: string | null;
}

export interface OperacionResponse {
  exitoso: boolean;
  mensaje: string;
}

// NUEVO: progreso en tiempo real del proceso de evaluación
export interface FaseProgresoUI {
  nombre:          string;
  orden:           number;
  peso:            number;
  estado:          'pendiente' | 'en_curso' | 'completada' | 'omitida';
  calificacion:    number | null;
  fechaCompletada: string | null;
}

export interface ProgresoPostulante {
  idProceso:          number;
  estadoGeneral:      string;
  faseActual:         string | null;
  progreso:           number;          // 0-100
  puntajeMatriz:      number | null;
  puntajeEntrevista:  number | null;
  puntajeFinal:       number | null;
  decision:           string | null;
  justificacion:      string | null;
  fases:              FaseProgresoUI[];
  sinProceso?:        boolean;
}

// ============================================================
// DocumentoService
// ============================================================
@Injectable({ providedIn: 'root' })
export class DocumentoService {

  private readonly API = `${environment.apiUrl}/documentos`;

  constructor(private http: HttpClient) {}

  // ─── Métodos existentes (sin cambios) ─────────────────────

  obtenerInfoPostulante(idUsuario: number): Observable<PostulanteInfo> {
    return this.http.get<PostulanteInfo>(`${this.API}/postulante/${idUsuario}`);
  }

  obtenerDocumentos(idPostulacion: number): Observable<DocumentoBackend[]> {
    return this.http.get<DocumentoBackend[]>(`${this.API}/postulacion/${idPostulacion}`);
  }

  validarDocumento(idDocumento: number, estado: 'validado' | 'rechazado', observacion: string): Observable<any> {
    return this.http.post(`${this.API}/validar/${idDocumento}`, { estado, observacion });
  }

  obtenerInfoPorPostulacion(idPostulacion: number): Observable<any> {
    return this.http.get(`${this.API}/info-postulacion/${idPostulacion}`);
  }

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

    const req = new HttpRequest('POST', `${this.API}/subir`, formData, { reportProgress: true });

    return new Observable(observer => {
      this.http.request(req).subscribe({
        next: event => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            progreso$.next(Math.round(100 * event.loaded / event.total));
          } else if (event instanceof HttpResponse) {
            observer.next(event.body as SubirDocumentoResponse);
            observer.complete();
          }
        },
        error: err => observer.error(err)
      });
    });
  }

  eliminarDocumento(idDocumento: number, idPostulacion: number): Observable<OperacionResponse> {
    return this.http.delete<OperacionResponse>(
      `${this.API}/${idDocumento}/postulacion/${idPostulacion}`
    );
  }

  finalizarCarga(idPostulacion: number): Observable<OperacionResponse> {
    return this.http.post<OperacionResponse>(`${this.API}/finalizar/${idPostulacion}`, {});
  }

  notificarRevision(idPostulacion: number): Observable<OperacionResponse> {
    return this.http.post<OperacionResponse>(`${this.API}/notificar-revision/${idPostulacion}`, {});
  }

  obtenerDocumentosConvocatoria(idPostulacion: number): Observable<DocumentoBackend[]> {
    return this.http.get<DocumentoBackend[]>(`${this.API}/convocatoria/${idPostulacion}`);
  }

  obtenerDocsPrepostulacion(idPostulacion: number): Observable<DocPrepostulacion[]> {
    return this.http.get<DocPrepostulacion[]>(`${this.API}/prepostulacion/${idPostulacion}`);
  }

  obtenerResultadosPostulante(idUsuario: number): Observable<any> {
    return this.http.get(`${this.API}/resultados/${idUsuario}`);
  }

  // ─── NUEVOS métodos ────────────────────────────────────────

  /**
   * Lista todas las postulaciones activas del usuario.
   * Usado para el selector de convocatoria en el header de cada módulo.
   */
  listarMisPostulaciones(idUsuario: number): Observable<PostulanteInfo[]> {
    return this.http.get<PostulanteInfo[]>(
      `${this.API}/postulante/${idUsuario}/postulaciones`
    );
  }

  /**
   * Info del postulante para una postulación específica (cuando el usuario filtra).
   */
  obtenerInfoPorConvocatoria(idUsuario: number, idPostulacion: number): Observable<PostulanteInfo> {
    return this.http.get<PostulanteInfo>(
      `${this.API}/postulante/${idUsuario}/postulacion/${idPostulacion}`
    );
  }

  /**
   * Progreso en tiempo real del proceso de evaluación.
   * El frontend hace polling con interval() para efecto "tiempo real".
   */
  obtenerMiProgreso(idUsuario: number, idPostulacion?: number): Observable<ProgresoPostulante> {
    const params: any = {};
    if (idPostulacion) params['idPostulacion'] = idPostulacion.toString();
    return this.http.get<ProgresoPostulante>(
      `${this.API}/postulante/${idUsuario}/progreso`,
      { params }
    );
  }
}

export interface DocPrepostulacion {
  idDocumento:  number;
  descripcion:  string;
  urlDocumento: string;
  fechaSubida:  string;
}
