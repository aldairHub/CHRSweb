import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http'; // Para conectar con backend si hiciera falta
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // Ajusta esto a tu backend real (ej: 'http://localhost:8080/api/auth')
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient, private router: Router) { }

  // --- LOGIN ---
  login(usuarioApp: string, claveApp: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { usuarioApp, claveApp });
  }

  // --- GUARDAR SESIÓN ---
  guardarSesion(datos: any): void {
    if (datos?.token) localStorage.setItem('token', datos.token);
    if (datos?.usuarioApp) localStorage.setItem('usuario', datos.usuarioApp);

    const roles: string[] = Array.isArray(datos?.roles) ? datos.roles : [];
    localStorage.setItem('roles', JSON.stringify(roles));

    const rolPrincipal = this.calcularRolPrincipal(roles);
    if (rolPrincipal) {
      localStorage.setItem('rol', rolPrincipal);
    } else {
      localStorage.removeItem('rol');
    }
  }

  // --- LOGOUT (CERRAR SESIÓN) ---
  logout(): void {
    // 1. Borramos todo rastro del usuario en el navegador
    localStorage.clear();

    // 2. (Opcional) Si tu backend necesita invalidar el token, descomenta esto:
    // this.http.post(`${this.apiUrl}/logout`, {}).subscribe();

    // 3. Mandamos al usuario al login
    this.router.navigate(['/login']);
  }

  // --- UTILIDADES ---
  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  getRol(): string | null {
    return localStorage.getItem('rol');
  }

  getRoles(): string[] {
    try {
      return JSON.parse(localStorage.getItem('roles') ?? '[]');
    } catch {
      return [];
    }
  }

  redirigirPorRol(): void {
    const rol = this.getRol();
    switch (rol) {
      case 'admin': this.router.navigate(['/admin']); break;
      case 'evaluador': this.router.navigate(['/evaluador']); break;
      case 'postulante': this.router.navigate(['/postulante']); break;
      default: this.router.navigate(['/login']); break;
    }
  }

  private calcularRolPrincipal(roles: string[]): 'admin' | 'evaluador' | 'postulante' | null {
    const r = (roles ?? []).map(x => (x || '').toUpperCase());

    // Reglas de prioridad
    if (r.some(role => role.includes('ADMIN'))) return 'admin';
    if (r.some(role => role.includes('EVALUADOR') || role.includes('EVALUATOR'))) return 'evaluador';
    if (r.includes('ROLE_POSTULANTE')) return 'postulante';

    return null;
  }
}
