import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './navbar';
import { FooterComponent } from './footer';
import { ToastComponent } from './toast.component';
import { filter } from 'rxjs/operators';

const RUTAS_SIN_LAYOUT = [
  '',
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
  imports: [
    CommonModule,
    FormsModule,
    RouterOutlet,
    NavbarComponent,
    FooterComponent,
    ToastComponent
  ],
  templateUrl: './app.component.html'
})
export class AppComponent {
  mostrarLayout = false;
  title = 'Portal UTEQ';

  constructor(private router: Router) {
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe((e: any) => {
      const url: string = e.urlAfterRedirects || e.url || '';
      this.mostrarLayout = !RUTAS_SIN_LAYOUT.some(
        r => url === r || url.startsWith(r + '?') || url.startsWith(r + '/')
      );
    });
  }
}
