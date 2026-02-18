// src/app/services/usuario-admin.service.ts

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

// Payload exacto que acepta AutoridadRegistroRequestDTO del backend
export interface AutoridadCreatePayload {
  nombres: string;
  apellidos: string;
  correo: string;
  fechaNacimiento: string | null;
  idInstitucion: number | null;
  rolesApp: string[];        // nombres de roles, no IDs
  idsRolAutoridad: number[];
}

// Payload para POST /api/admin/usuarios → sp_registrar_usuario_simple
// Genera credenciales automáticamente y envía correo
export interface UsuarioCreatePayload {
  correo: string;
  nombres: string;
  apellidos: string;
  rolesApp: string[];        // nombres de roles, no IDs
}

@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {

  private readonly api            = 'http://localhost:8080/api/admin';
  private readonly apiAutoridades = 'http://localhost:8080/api/autoridades-academicas';

  constructor(private http: HttpClient) {}

  // --- USUARIOS: /api/admin/usuarios --------------------------

  listarUsuarios(): Observable<UsuarioConRolesDTO[]> {
    return this.http.get<UsuarioConRolesDTO[]>(`${this.api}/usuarios`);
  }

  /**
   * POST /api/admin/usuarios
   * Usa sp_registrar_usuario_simple: genera credenciales, crea user en PG,
   * asigna roles BD y envia correo automaticamente.
   */
  crearUsuario(payload: UsuarioCreatePayload): Observable<any> {
    return this.http.post(`${this.api}/usuarios`, payload);
  }

  /** PATCH /api/admin/usuarios/{id}/estado?activo=true|false */
  cambiarEstadoUsuario(id: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(
      `${this.api}/usuarios/${id}/estado`,
      null,
      { params: { activo: String(activo) } }
    );
  }

  /** PUT /api/admin/usuarios/{id}/roles — body: { idsRolApp: [...] } */
  actualizarRolesUsuario(id: number, idsRolApp: number[]): Observable<UsuarioConRolesDTO> {
    return this.http.put<UsuarioConRolesDTO>(
      `${this.api}/usuarios/${id}/roles`,
      { idsRolApp }
    );
  }

  // --- AUTORIDADES: /api/admin/autoridades --------------------

  listarAutoridades(): Observable<AutoridadConRolesDTO[]> {
    return this.http.get<AutoridadConRolesDTO[]>(`${this.api}/autoridades`);
  }

  /**
   * POST /api/autoridades-academicas/registro
   * Genera usuario, crea credenciales y envia correo.
   */
  crearAutoridad(payload: AutoridadCreatePayload): Observable<any> {
    return this.http.post(`${this.apiAutoridades}/registro`, payload);
  }

  /** PATCH /api/admin/autoridades/{id}/estado?estado=true|false */
  cambiarEstadoAutoridad(id: number, estado: boolean): Observable<void> {
    return this.http.patch<void>(
      `${this.api}/autoridades/${id}/estado`,
      null,
      { params: { estado: String(estado) } }
    );
  }

  /** PUT /api/admin/autoridades/{id}/roles — body: { idsRolApp: [...] } */
  actualizarRolesAutoridad(id: number, idsRolApp: number[]): Observable<AutoridadConRolesDTO> {
    return this.http.put<AutoridadConRolesDTO>(
      `${this.api}/autoridades/${id}/roles`,
      { idsRolApp }
    );
  }
}
