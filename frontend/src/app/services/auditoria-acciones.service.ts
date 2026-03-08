// src/app/services/auditoria-acciones.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface UsuarioAudit {
  idUsuario: number;
  usuarioApp: string;
  usuarioBd: string;
  correo: string;
}

export interface AudAccion {
  idAudAccion: number;
  usuario: UsuarioAudit | null;
  usuarioApp: string;
  usuarioBd: string;
  accion: string;
  entidad: string;
  idEntidad: number | null;
  descripcion: string | null;
  ipCliente: string | null;
  fecha: string;
}

export interface PageAudAccion {
  content: AudAccion[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface FiltrosAuditoria {
  usuarioApp?: string;
  usuarioBd?: string;
  accion?: string;
  entidad?: string;
  desde?: string;
  hasta?: string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaAccionesService {

  private readonly base = `${environment.apiUrl}/admin/auditoria/acciones`;

  constructor(private http: HttpClient) {}

  listar(filtros: FiltrosAuditoria, page = 0, size = 20): Observable<PageAudAccion> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (filtros.usuarioApp?.trim()) params = params.set('usuarioApp', filtros.usuarioApp.trim());
    if (filtros.usuarioBd?.trim())  params = params.set('usuarioBd',  filtros.usuarioBd.trim());
    if (filtros.accion)             params = params.set('accion',     filtros.accion);
    if (filtros.entidad)            params = params.set('entidad',    filtros.entidad);
    if (filtros.desde)              params = params.set('desde',      filtros.desde);
    if (filtros.hasta)              params = params.set('hasta',      filtros.hasta);

    return this.http.get<PageAudAccion>(this.base, { params });
  }
}
