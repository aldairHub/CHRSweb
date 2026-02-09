import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient, private router: Router) {}

  login(usuarioApp: string, claveApp: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { usuarioApp, claveApp });
  }

  guardarSesion(datos: any): void {
    if (datos?.token) localStorage.setItem('token', datos.token);
    if (datos?.usuarioApp) localStorage.setItem('usuario', datos.usuarioApp);

    const roles: string[] = Array.isArray(datos?.roles) ? datos.roles : [];
    localStorage.setItem('roles', JSON.stringify(roles));

    const rolPrincipal = this.calcularRolPrincipal(roles);
    if (rolPrincipal) localStorage.setItem('rol', rolPrincipal);
    else localStorage.removeItem('rol');
  }

  /** 1) SOLO backend: llama endpoint logout y RETORNA observable */
  logoutBackend(): Observable<any> {
    const token = localStorage.getItem('token');

    // Si no hay token, no tiene sentido pegarle al backend
    if (!token) return of(null);

    const headers = new HttpHeaders({
      Authorization: `Bearer ${token}`
    });

    return this.http.post(`${this.apiUrl}/logout`, {}, { headers }).pipe(
      // si el backend falla igual dejamos salir al usuario
      catchError(() => of(null))
    );
  }

  /** 2) SOLO local: limpia storage y navega */
  cerrarSesionLocal(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  /** 3) Helper opcional: backend + local */
  logoutYSalir(): void {
    this.logoutBackend()
      .pipe(finalize(() => this.cerrarSesionLocal()))
      .subscribe();
  }
  //un cambio

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
    if (r.some(role => role.includes('ADMIN'))) return 'admin';
    if (r.some(role => role.includes('EVALUADOR') || role.includes('EVALUATOR'))) return 'evaluador';
    if (r.includes('ROLE_POSTULANTE')) return 'postulante';
    return null;
  }
}
