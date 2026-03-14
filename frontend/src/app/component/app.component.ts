import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './navbar';
import { FooterComponent } from './footer';
import { ToastComponent } from './toast.component';
import { filter } from 'rxjs/operators';
import { LogoService } from '../services/logo.service';

// Rutas donde NUNCA aparece el navbar (aunque haya token)
const RUTAS_SIN_NAVBAR: string[] = [
  '/',
  '/login',
  '/registro',
  '/recuperar-clave',
  '/cambiar-clave-obligatorio',
  '/convocatorias',
  '/repostulacion'
];

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterOutlet, NavbarComponent, FooterComponent, ToastComponent],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  mostrarLayout = false;
  title = 'Portal';

  constructor(private router: Router, private logoService: LogoService) {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => this.evaluar(e.urlAfterRedirects || e.url || ''));
  }

  ngOnInit(): void {
    // Limpiar tokens huérfanos al iniciar: si uno existe pero el otro no,
    // la sesión quedó a medias — limpiar todo para forzar login
    this.limpiarSesionHuerfana();

    this.evaluar(this.router.url);

    // Escuchar cambios de storage en otras pestañas (logout en tab A → ocultar navbar en tab B)
    window.addEventListener('storage', (e) => {
      if (e.key === 'token' || e.key === 'authState') {
        this.evaluar(this.router.url);
      }
    });

    // Título de pestaña = siglas si existen, si no appName
    this.actualizarTitulo();

    // Actualizar cuando el servicio refresca desde la API
    this.logoService.getNombreCorto().subscribe(() => this.actualizarTitulo());
    this.logoService.getNombre().subscribe(() => this.actualizarTitulo());
  }

  private limpiarSesionHuerfana(): void {
    const token     = localStorage.getItem('token');
    const authState = localStorage.getItem('authState');

    // Si hay authState pero no token (o token vacío), la sesión quedó rota
    if (authState && !token) {
      localStorage.removeItem('authState');
      return;
    }

    // Si hay token pero no authState, también es sesión huérfana
    if (token && !authState) {
      localStorage.removeItem('token');
      localStorage.removeItem('rol');
      localStorage.removeItem('roles');
      localStorage.removeItem('usuario');
      localStorage.removeItem('idUsuario');
      localStorage.removeItem('modulo');
      return;
    }

    // Si hay ambos, verificar que el token dentro de authState coincida con el token guardado
    if (token && authState) {
      try {
        const estado = JSON.parse(authState);
        if (estado?.token && estado.token !== token) {
          // Token desincronizado — limpiar todo
          localStorage.clear();
        }
      } catch {
        localStorage.clear();
      }
    }
  }

  private actualizarTitulo(): void {
    const siglas  = localStorage.getItem('inst_nombreCorto');
    const appName = localStorage.getItem('inst_appName');
    document.title = siglas || appName || 'SSDC';
  }

  private evaluar(url: string): void {
    const ruta = url.split('?')[0].split('#')[0] || '/';

    const esPublica = RUTAS_SIN_NAVBAR.some(r =>
      ruta === r || ruta.startsWith(r === '/' ? '/#' : r + '/') || ruta.startsWith(r + '?')
    );

    // Sesión válida = token Y authState deben existir ambos
    const hayToken     = !!localStorage.getItem('token');
    const hayAuthState = !!localStorage.getItem('authState');
    const sesionValida = hayToken && hayAuthState;

    this.mostrarLayout = !esPublica && sesionValida;
  }
}
