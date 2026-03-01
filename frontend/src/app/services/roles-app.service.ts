// src/app/services/roles-app.service.ts
//
// Reemplaza a rol-autoridad.service.ts
// Consume los nuevos endpoints /api/roles-app y /api/roles-app/roles-bd-disponibles

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RolAppDTO {
  idRolApp: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  fechaCreacion: string;
}

export interface RolAppConRolesBdDTO {
  idRolApp: number;
  nombre: string;
  descripcion: string;
  activo: boolean;
  fechaCreacion: string;
  rolesBd: string[];  // p.ej. ["role_admin_bd", "role_lecturas"]
  idModulo: number | null;
  nombreModulo: string | null;
}

export interface RolAppSavePayload {
  nombre: string;
  descripcion?: string;
  activo?: boolean;
  rolesBd: string[];
  idModulo?: number | null;
}

@Injectable({ providedIn: 'root' })
export class RolesAppService {
  private readonly api = 'http://localhost:8080/api/roles-app';

  constructor(private http: HttpClient) {}

  /** Lista todos los roles_app con sus roles_bd (para la tabla). */
  listar(): Observable<RolAppConRolesBdDTO[]> {
    return this.http.get<RolAppConRolesBdDTO[]>(this.api);
  }

  /** Lista roles de BD disponibles desde pg_roles (prefijo role_*, NOLOGIN). */
  listarRolesBdDisponibles(): Observable<string[]> {
    return this.http.get<string[]>(`${this.api}/roles-bd-disponibles`);
  }

  /** Crea un rol_app. */
  crear(payload: RolAppSavePayload): Observable<RolAppConRolesBdDTO> {
    return this.http.post<RolAppConRolesBdDTO>(this.api, payload);
  }

  /** Actualiza un rol_app. */
  actualizar(id: number, payload: RolAppSavePayload): Observable<RolAppConRolesBdDTO> {
    return this.http.put<RolAppConRolesBdDTO>(`${this.api}/${id}`, payload);
  }

  /** Activa o desactiva un rol_app. */
  cambiarEstado(id: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(`${this.api}/${id}/estado`, null, {
      params: { activo: String(activo) }
    });
  }
  listarModulos(): Observable<any[]> {
    return this.http.get<any[]>('http://localhost:8080/api/admin/opciones/modulos');
  }
}
