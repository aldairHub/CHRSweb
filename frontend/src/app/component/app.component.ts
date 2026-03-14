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
    this.evaluar(this.router.url);

    // Título de pestaña = siglas si existen, si no appName
    this.actualizarTitulo();

    // Actualizar cuando el servicio refresca desde la API
    this.logoService.getNombreCorto().subscribe(() => this.actualizarTitulo());
    this.logoService.getNombre().subscribe(() => this.actualizarTitulo());
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

    const hayToken = !!localStorage.getItem('token');
    this.mostrarLayout = !esPublica && hayToken;
  }
}
