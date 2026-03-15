// src/app/services/usuario-admin.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RolAppDTO } from './roles-app.service';

export interface UsuarioConRolesDTO {
  idUsuario:     number;
  usuarioApp:    string;
  usuarioBd:     string;
  correo:        string;
  activo:        boolean;
  fechaCreacion: string;
  rolesApp:      RolAppDTO[];
}

export interface AutoridadConRolesDTO {
  idAutoridad:     number;
  nombres:         string;
  apellidos:       string;
  correo:          string;
  fechaNacimiento: string;
  estado:          boolean;
  idInstitucion:   number;
  idUsuario:       number;
  usuarioApp:      string;
  usuarioBd:       string;
  rolesApp:        RolAppDTO[];
  fotoPerfil:     string | null;
}

/** DTO de postulante para la vista admin (solo lectura) */
export interface PostulanteAdminDTO {
  idPostulante:   number;
  nombres:        string;
  apellidos:      string;
  identificacion: string;
  correo:         string;
  usuarioApp:     string | null;
  activo:         boolean | null;
  fotoPerfil:     string | null;
}

export interface AutoridadCreatePayload {
  nombres:         string;
  apellidos:       string;
  correo:          string;
  fechaNacimiento: string | null;
  idInstitucion:   number | null;
  idFacultad?:     number | null;
  rolesApp:        string[];
  idsRolAutoridad: number[];
}

export interface UsuarioCreatePayload {
  correo:    string;
  nombres:   string;
  apellidos: string;
  rolesApp:  string[];
}

@Injectable({ providedIn: 'root' })
export class UsuarioAdminService {

  private readonly api            = 'http://localhost:8080/api/admin';
  private readonly apiAutoridades = 'http://localhost:8080/api/autoridades-academicas';

  constructor(private http: HttpClient) {}

  // --- USUARIOS: /api/admin/usuarios ----------------------------------

  listarUsuarios(): Observable<UsuarioConRolesDTO[]> {
    return this.http.get<UsuarioConRolesDTO[]>(`${this.api}/usuarios`);
  }

  crearUsuario(payload: UsuarioCreatePayload): Observable<any> {
    return this.http.post(`${this.api}/usuarios`, payload);
  }

  cambiarEstadoUsuario(id: number, activo: boolean): Observable<void> {
    return this.http.patch<void>(
      `${this.api}/usuarios/${id}/estado`,
      null,
      { params: { activo: String(activo) } }
    );
  }

  actualizarRolesUsuario(id: number, idsRolApp: number[]): Observable<UsuarioConRolesDTO> {
    return this.http.put<UsuarioConRolesDTO>(
      `${this.api}/usuarios/${id}/roles`,
      { idsRolApp }
    );
  }

  // --- AUTORIDADES: /api/admin/autoridades ----------------------------

  listarAutoridades(): Observable<AutoridadConRolesDTO[]> {
    return this.http.get<AutoridadConRolesDTO[]>(`${this.api}/autoridades`);
  }

  crearAutoridad(payload: AutoridadCreatePayload): Observable<any> {
    return this.http.post(`${this.apiAutoridades}/registro`, payload);
  }

  cambiarEstadoAutoridad(id: number, estado: boolean): Observable<void> {
    return this.http.patch<void>(
      `${this.api}/autoridades/${id}/estado`,
      null,
      { params: { estado: String(estado) } }
    );
  }

  actualizarRolesAutoridad(id: number, idsRolApp: number[]): Observable<AutoridadConRolesDTO> {
    return this.http.put<AutoridadConRolesDTO>(
      `${this.api}/autoridades/${id}/roles`,
      { idsRolApp }
    );
  }

  // --- NUEVO: POSTULANTES (solo lectura) ------------------------------

  /** GET /api/admin/postulantes — lista completa de postulantes registrados. */
  listarPostulantes(): Observable<PostulanteAdminDTO[]> {
    return this.http.get<PostulanteAdminDTO[]>(`${this.api}/postulantes`);
  }
}
