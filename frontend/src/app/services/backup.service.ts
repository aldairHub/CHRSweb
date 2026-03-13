import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConfigBackup {
  idConfig:        number;
  rutaOrigen:      string;
  rutaDestino:     string;
  tipoBackup:      string;
  diasRetencion:   number;
  horaBackup1:     string;
  horaBackup2:     string;
  activo:          boolean;
  notificarError:  boolean;
  notificarExito:  boolean;
}

export interface HistorialBackup {
  idHistorial:        number;
  estado:             string;
  tipoBackup:         string;
  rutaArchivo:        string;
  tamanoBytes:        number;
  duracionSegundos:   number;
  mensajeError:       string;
  origen:             string;
  fechaInicio:        string;
  fechaFin:           string;
  tamanoFormateado:   string;
  duracionFormateada: string;
}

@Injectable({ providedIn: 'root' })
export class BackupService {
  private readonly api = '/api/backup';

  constructor(private http: HttpClient) {}

  obtenerConfig(): Observable<ConfigBackup> {
    return this.http.get<ConfigBackup>(`${this.api}/config`);
  }

  guardarConfig(config: ConfigBackup): Observable<ConfigBackup> {
    return this.http.put<ConfigBackup>(`${this.api}/config`, config);
  }

  obtenerHistorial(): Observable<HistorialBackup[]> {
    return this.http.get<HistorialBackup[]>(`${this.api}/historial`);
  }

  ejecutarManual(): Observable<HistorialBackup> {
    return this.http.post<HistorialBackup>(`${this.api}/ejecutar`, {});
  }
}
