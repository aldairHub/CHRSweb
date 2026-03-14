// src/app/services/auditoria-cambios.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AudCambio {
  idAudCambio:  number;
  tabla:        string;
  idRegistro:   number;
  operacion:    'INSERT' | 'UPDATE' | 'DELETE';
  campo:        string;
  valorAntes:   string | null;
  valorDespues: string | null;
  usuarioBd:    string;
  usuarioApp:   string | null;
  ipCliente:    string | null;
  fecha:        string;
}

export interface PageAudCambio {
  content:       AudCambio[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
}

export interface FiltrosCambios {
  tabla?:      string;
  operacion?:  string;
  campo?:      string;
  usuarioApp?: string;
  desde?:      string;
  hasta?:      string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaCambiosService {

  private readonly base = `${environment.apiUrl}/admin/auditoria/cambios`;

  constructor(private http: HttpClient) {}

  listar(filtros: FiltrosCambios, page = 0, size = 20): Observable<PageAudCambio> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filtros.tabla?.trim())      params = params.set('tabla',      filtros.tabla.trim());
    if (filtros.operacion)          params = params.set('operacion',  filtros.operacion);
    if (filtros.campo?.trim())      params = params.set('campo',      filtros.campo.trim());
    if (filtros.usuarioApp?.trim()) params = params.set('usuarioApp', filtros.usuarioApp.trim());
    if (filtros.desde)              params = params.set('desde',      filtros.desde);
    if (filtros.hasta)              params = params.set('hasta',      filtros.hasta);

    return this.http.get<PageAudCambio>(this.base, { params });
  }
}
