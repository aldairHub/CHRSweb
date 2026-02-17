// src/app/services/usuario-admin.service.ts
//
// Nuevo servicio para la pantalla "Gestión de Usuarios" (pestañas Usuarios / Autoridades).
// Reemplaza el uso de autoridad-academica.service.ts en gestion-usuarios.

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RolAppDTO } from './roles-app.service';

export interface UsuarioConRolesDTO {
  idUsuario: number;
  usuarioApp: string;
  usuarioBd: string;
  correo: string;
  activo: boolean;
  fechaCreacion: string;
  rolesApp: RolAppDTO[];
}

export interface AutoridadConRolesDTO {
  idAutoridad: number;
  nombres: string;
  apellidos: string;
  correo: string;
  fechaNacimiento: string;
  estado: boolean;
  idInstitucion: number;
  idUsuario: number;
  usuarioApp: string;
  usuarioBd: string;
  rolesApp: RolAppDTO[];
}

@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {
  private readonly api = 'http://localhost:8080/api/admin';

  constructor(private http: HttpClient) {}

  // ─── Usuarios ───────────────────────────────────────────────

  listarUsuarios(): Observable<UsuarioConRolesDTO[]> {
    return this.http.get<UsuarioConRolesDTO[]>(`${this.api}/usuarios`);
  }

  cambiarEstadoUsuario(id: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(`${this.api}/usuarios/${id}/estado`, null, {
      params: { activo: String(activo) }
    });
  }

  actualizarRolesUsuario(id: number, idsRolApp: number[]): Observable<UsuarioConRolesDTO> {
    return this.http.put<UsuarioConRolesDTO>(`${this.api}/usuarios/${id}/roles`, { idsRolApp });
  }

  // ─── Autoridades ────────────────────────────────────────────

  listarAutoridades(): Observable<AutoridadConRolesDTO[]> {
    return this.http.get<AutoridadConRolesDTO[]>(`${this.api}/autoridades`);
  }

  cambiarEstadoAutoridad(id: number, estado: boolean): Observable<void> {
    return this.http.patch<void>(`${this.api}/autoridades/${id}/estado`, null, {
      params: { estado: String(estado) }
    });
  }

  actualizarRolesAutoridad(id: number, idsRolApp: number[]): Observable<AutoridadConRolesDTO> {
    return this.http.put<AutoridadConRolesDTO>(`${this.api}/autoridades/${id}/roles`, { idsRolApp });
  }
}
