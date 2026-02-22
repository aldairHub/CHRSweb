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

  guardarSesion(datos: any, recordarme: boolean): void {
    if (datos?.token) localStorage.setItem('token', datos.token);
    if (datos?.usuarioApp) localStorage.setItem('usuario', datos.usuarioApp);

    const roles: string[] = Array.isArray(datos?.roles) ? datos.roles : [];
    localStorage.setItem('roles', JSON.stringify(roles));

    const rolPrincipal = this.calcularRolPrincipal(roles);
    if (rolPrincipal) localStorage.setItem('rol', rolPrincipal);
    else localStorage.removeItem('rol');
    //  nuevo — guardar primerLogin
    localStorage.setItem('primerLogin', datos?.primerLogin ? 'true' : 'false');
  }
  //  nuevo — leer primerLogin
  esPrimerLogin(): boolean {
    return localStorage.getItem('primerLogin') === 'true';
  }

  //  nuevo — limpiar flag después del cambio
  limpiarPrimerLogin(): void {
    localStorage.setItem('primerLogin', 'false');
  }

  /** 1) SOLO backend: llama endpoint logout y RETORNA observable */
  logoutBackend(): Observable<any> {
    const token = localStorage.getItem('token');
    if (!token) return of(null);

    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });
    return this.http.post(`${this.apiUrl}/logout`, {}, { headers }).pipe(
      catchError(() => of(null))
    );
  }

  /** 2) SOLO local: limpia storage y navega */
  cerrarSesionLocal(): void {
    localStorage.clear();
    this.router.navigate(['/login']);
  }

  /** 3) Helper: backend + local */
  logoutYSalir(): void {
    this.logoutBackend()
      .pipe(finalize(() => this.cerrarSesionLocal()))
      .subscribe();
  }

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
    //  Si es primer login, redirigir a cambio de clave obligatorio
    if (this.esPrimerLogin()) {
      this.router.navigate(['/cambiar-clave-obligatorio'], { replaceUrl: true });
      return;
    }
    const rol = this.getRol();

    switch (rol) {
      case 'admin':
        this.router.navigate(['/admin'], { replaceUrl: true });
        break;

      case 'evaluador':
        this.router.navigate(['/evaluador'], { replaceUrl: true });
        break;

      case 'postulante':
        this.router.navigate(['/postulante'], { replaceUrl: true });
        break;

      // ✅ NUEVO: rol REVISOR → módulo vicerrectorado
      case 'revisor':
        this.router.navigate(['/revisor'], { replaceUrl: true });
        break;

      default:
        // Rol desconocido → pantalla de acceso no configurado
        this.router.navigate(['/sin-acceso'], { replaceUrl: true });
        break;
    }
  }

  /**
   * Mapea los roles del backend (strings en mayúsculas) al rol local
   * que usa el frontend para rutas y guards.
   *
   * PRIORIDAD: admin > revisor > evaluador > postulante
   */
  private calcularRolPrincipal(
    roles: string[]
  ): 'admin' | 'evaluador' | 'postulante' | 'revisor' | null {
    const r = (roles ?? []).map(x => (x || '').toUpperCase());

    if (r.some(role => role.includes('ADMIN'))) return 'admin';
    if (r.some(role => role.includes('REVISOR'))) return 'revisor';
    if (r.some(role => role.includes('EVALUADOR') || role.includes('EVALUATOR'))) return 'evaluador';
    if (r.includes('ROLE_POSTULANTE') || r.includes('POSTULANTE')) return 'postulante';

    return null;
  }
}
