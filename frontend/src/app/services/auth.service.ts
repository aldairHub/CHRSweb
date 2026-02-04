import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient, private router: Router) { }

  login(usuarioApp: string, claveApp: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { usuarioApp, claveApp });
  }

  guardarSesion(datos: any): void {
    // 1) Token y usuario
    if (datos?.token) localStorage.setItem('token', datos.token);
    if (datos?.usuarioApp) localStorage.setItem('usuario', datos.usuarioApp);

    // 2) Guardar roles completos (recomendado)
    const roles: string[] = Array.isArray(datos?.roles) ? datos.roles : [];
    localStorage.setItem('roles', JSON.stringify(roles));

    // 3) Guardar rol principal "simple" para tu routing actual (admin/evaluador/postulante)
    const rolPrincipal = this.calcularRolPrincipal(roles);
    if (rolPrincipal) {
      localStorage.setItem('rol', rolPrincipal); // admin | evaluador | postulante
    } else {
      localStorage.removeItem('rol');
    }
  }

  private calcularRolPrincipal(roles: string[]): 'admin' | 'evaluador' | 'postulante' | null {
    const r = (roles ?? []).map(x => (x || '').toUpperCase());

    const isAdmin =
      r.includes('ROLE_SUPER_ADMIN') ||
      r.includes('ROLE_ADMIN') ||
      r.includes('ROLE_SECURITY_ADMIN') ||
      r.includes('ROLE_MASTERDATA_ADMIN') ||
      r.includes('ROLE_CONVOCATORIA_ADMIN');

    if (isAdmin) return 'admin';

    if (r.includes('ROLE_EVALUATOR') || r.includes('ROLE_EVALUADOR')) return 'evaluador';

    if (r.includes('ROLE_POSTULANTE')) return 'postulante';

    return null;
  }

  getRol(): string | null {
    return localStorage.getItem('rol'); // admin|evaluador|postulante
  }

  getRoles(): string[] {
    try {
      return JSON.parse(localStorage.getItem('roles') ?? '[]');
    } catch {
      return [];
    }
  }

  isLoggedIn(): boolean {
    // mejor chequear token
    return localStorage.getItem('token') !== null;
  }

  logout(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
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
}
