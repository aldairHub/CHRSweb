import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface TipoDocumento {
  idTipoDocumento: number;
  nombre:          string;
  descripcion:     string;
  obligatorio:     boolean;
  activo:          boolean;
}

@Injectable({ providedIn: 'root' })
export class TipoDocumentoService {

  private readonly API = `${environment.apiUrl}/tipos-documento`;

  constructor(private http: HttpClient) {}

  listar(): Observable<TipoDocumento[]> {
    return this.http.get<TipoDocumento[]>(this.API);
  }

  crear(data: { nombre: string; descripcion: string; obligatorio: boolean }): Observable<any> {
    return this.http.post(this.API, data);
  }

  editar(id: number, data: { nombre: string; descripcion: string; obligatorio: boolean }): Observable<any> {
    return this.http.put(`${this.API}/${id}`, data);
  }

  toggle(id: number): Observable<any> {
    return this.http.patch(`${this.API}/${id}/toggle`, {});
  }
}
