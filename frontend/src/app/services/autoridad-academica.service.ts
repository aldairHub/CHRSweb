// src/app/services/autoridad-academica.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InstitucionDto {
  idInstitucion: number;
  nombre: string;
}

export interface RolAutoridadDto {
  idRolAutoridad: number;
  nombre: string;
}

export interface AutoridadResponseDto {
  idAutoridad: number;
  nombres: string;
  apellidos: string;
  correo: string;
  fechaNacimiento: string; // YYYY-MM-DD
  estado: boolean;
  idUsuario: number;
  idInstitucion: number;
  rolesAutoridad: RolAutoridadDto[];
}

export interface AutoridadRegistroPayload {
  nombres: string;
  apellidos: string;
  correo: string;
  fechaNacimiento: string; // YYYY-MM-DD
  idInstitucion: number;
  idsRolAutoridad: number[];
}

export interface AutoridadRegistroResponse {
  idAutoridad: number;
  idUsuario: number;
  usuarioApp: string;
  usuarioBd: string;
}

export interface AutoridadUpdatePayload {
  nombres: string;
  apellidos: string;
  correo: string;
  fechaNacimiento: string; // YYYY-MM-DD
  estado: boolean;
  idUsuario: number;
  idInstitucion: number;
  idsRolAutoridad: number[];
}

export interface RolUsuarioResponseDto {
  idRolUsuario: number;
  nombre: string;
}

@Injectable({ providedIn: 'root' })
export class AutoridadAcademicaService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  listarAutoridades(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/autoridades-academicas`);
  }

  listarInstituciones(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/instituciones`);
  }

  listarCargosAutoridad(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/roles-autoridad`);
  }

  registrarAutoridad(payload: any): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/autoridades-academicas/registro`, payload);
  }

  cambiarEstadoAutoridad(idAutoridad: number, estado: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/autoridades-academicas/${idAutoridad}/estado`, { estado });
  }

  listarRolesUsuarioPorRolesAutoridad(idsRolAutoridad: number[]): Observable<RolUsuarioResponseDto[]> {
    return this.http.post<RolUsuarioResponseDto[]>(
      `${this.baseUrl}/roles-autoridad/roles-usuario`,
      { idsRolAutoridad }
    );
  }
}

