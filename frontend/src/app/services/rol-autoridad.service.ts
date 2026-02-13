import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RolAutoridadDto, RolAutoridadSavePayload, RolUsuarioDto } from '../models/rol-autoridad.model';

@Injectable({ providedIn: 'root' })
export class RolAutoridadRolesService {
  // ajusta base si usas proxy o env
  private apiUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // ✅ catálogo simple (id + nombre) si lo sigues usando en algún select
  listarRolesAutoridad(): Observable<RolAutoridadDto[]> {
    return this.http.get<RolAutoridadDto[]>(`${this.apiUrl}/roles-autoridad`);
  }

  // ✅ listado para la TABLA del módulo (incluye rolesUsuario)
  listarConRolesUsuario(): Observable<RolAutoridadDto[]> {
    return this.http.get<RolAutoridadDto[]>(`${this.apiUrl}/roles-autoridad/con-roles-usuario`);
  }

  // ✅ catálogo de rol_usuario para los checkboxes
  listarRolesUsuario(): Observable<RolUsuarioDto[]> {
    return this.http.get<RolUsuarioDto[]>(`${this.apiUrl}/roles-usuario`);
  }

  // ✅ crear rol_autoridad + relaciones
  crearRolAutoridad(payload: RolAutoridadSavePayload): Observable<any> {
    return this.http.post(`${this.apiUrl}/roles-autoridad`, payload);
  }

  // ✅ actualizar rol_autoridad + relaciones
  actualizarRolAutoridad(idRolAutoridad: number, payload: RolAutoridadSavePayload): Observable<any> {
    return this.http.put(`${this.apiUrl}/roles-autoridad/${idRolAutoridad}`, payload);
  }
}
