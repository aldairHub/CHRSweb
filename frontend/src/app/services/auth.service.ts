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
    if (datos?.idUsuario) localStorage.setItem('idUsuario', String(datos.idUsuario));

    localStorage.setItem('primerLogin', datos?.primerLogin ? 'true' : 'false');
    //  nuevo — guardar primerLogin

    const roles: string[] = Array.isArray(datos?.roles) ? datos.roles : [];
    localStorage.setItem('roles', JSON.stringify(roles));

    // Guardar módulo y opciones
    if (datos?.modulo) {
      localStorage.setItem("modulo",
        JSON.stringify(datos.modulo));
    }
    //
    // const rolPrincipal = this.calcularRolPrincipal(roles);
    // if (rolPrincipal) localStorage.setItem('rol', rolPrincipal);
    // else localStorage.removeItem('rol');

    // "rol" sigue siendo la clave de navegación (admin/evaluador/etc.) para el guard
    const rutaNavegacion = datos?.modulo?.moduloRuta
      ? datos.modulo.moduloRuta.replace(/^\//, '')   // "/admin" → "admin"
      : this.calcularRolPrincipal(roles);
    if (rutaNavegacion) localStorage.setItem("rol", rutaNavegacion);
    else                localStorage.removeItem("rol");

    if (datos?.nombreRolApp) {
      localStorage.setItem("rolNombre", datos.nombreRolApp);
    } else {
      localStorage.removeItem("rolNombre");
    }

  // guardar la lista de roles disponibles para el selector
    if (datos?.rolesDisponibles) {
      localStorage.setItem("rolesDisponibles", JSON.stringify(datos.rolesDisponibles));
    }
  }
  // Leer módulo y opciones del localStorage
  getModulo(): {
    moduloNombre: string;
    moduloRuta:   string;
    opciones:     any[];
  } | null {
    try {
      const raw = localStorage.getItem("modulo");
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  //  leer primerLogin
  esPrimerLogin(): boolean {
    return localStorage.getItem('primerLogin') === 'true';
  }

  //  limpiar flag después del cambio
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
  getRolNombre(): string | null {
    return localStorage.getItem('rolNombre');
  }

  getRolesDisponibles(): any[] {
    try {
      return JSON.parse(localStorage.getItem('rolesDisponibles') ?? '[]');
    } catch {
      return [];
    }
  }
  obtenerMenuPorRol(idRolApp: number): Observable<any> {
    return this.http.get(`/api/auth/menu/${idRolApp}`);
  }

  // redirigirPorRol(): void {
  //   //  Si es primer login, redirigir a cambio de clave obligatorio
  //   if (this.esPrimerLogin()) {
  //     this.router.navigate(['/cambiar-clave-obligatorio'], { replaceUrl: true });
  //     return;
  //   }
  //   const rol = this.getRol();
  //
  //   switch (rol) {
  //     case 'admin':
  //       this.router.navigate(['/admin'], { replaceUrl: true });
  //       break;
  //
  //     case 'evaluador':
  //       this.router.navigate(['/evaluador'], { replaceUrl: true });
  //       break;
  //
  //     case 'postulante':
  //       this.router.navigate(['/postulante'], { replaceUrl: true });
  //       break;
  //
  //     // ✅ NUEVO: rol REVISOR → módulo vicerrectorado
  //     case 'revisor':
  //       this.router.navigate(['/revisor'], { replaceUrl: true });
  //       break;
  //
  //     default:
  //       // Rol desconocido → pantalla de acceso no configurado
  //       this.router.navigate(['/sin-acceso'], { replaceUrl: true });
  //       break;
  //   }
  // }

  /**
   * Mapea los roles del backend (strings en mayúsculas) al rol local
   * que usa el frontend para rutas y guards.
   *
   * PRIORIDAD: admin > revisor > evaluador > postulante
   */
  // private calcularRolPrincipal(
  //   roles: string[]
  // ): 'admin' | 'evaluador' | 'postulante' | 'revisor' | null {
  //   const r = (roles ?? []).map(x => (x || '').toUpperCase());
  //
  //   if (r.some(role => role.includes('ADMIN'))) return 'admin';
  //   if (r.some(role => role.includes('REVISOR'))) return 'revisor';
  //   if (r.some(role => role.includes('EVALUADOR') || role.includes('EVALUATOR'))) return 'evaluador';
  //   if (r.includes('ROLE_POSTULANTE') || r.includes('POSTULANTE')) return 'postulante';
  //
  //   return null;
  // }
  private readonly ROL_A_RUTA: Array<{ contiene: string; ruta: string }> = [
    { contiene: "ADMIN",      ruta: "admin"      },
    { contiene: "REVISOR",    ruta: "revisor"    },
    { contiene: "EVALUADOR",  ruta: "evaluador"  },
    { contiene: "POSTULANTE", ruta: "postulante" },
    // Para un rol nuevo: agregar aquí una sola línea
    // { contiene: "DIRECTOR", ruta: "director" },
  ];

  private calcularRolPrincipal(roles: string[]): string | null {
    const rolesUpper = (roles ?? []).map(r => (r || "").toUpperCase());
    for (const cfg of this.ROL_A_RUTA) {
      if (rolesUpper.some(r => r.includes(cfg.contiene))) return cfg.ruta;
    }
    return null;
  }

  redirigirPorRol(): void {
    if (this.esPrimerLogin()) {
      this.router.navigate(["/cambiar-clave-obligatorio"],
        { replaceUrl: true });
      return;
    }
    const ruta = this.getRol();
    if (ruta) this.router.navigate([`/${ruta}`], { replaceUrl: true });
    else      this.router.navigate(["/sin-acceso"],  { replaceUrl: true });
  }


}
