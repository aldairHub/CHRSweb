import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NavbarComponent } from './component/navbar';
import { FooterComponent } from './component/footer';
import { ToastComponent } from './component/toast.component';
import { ThemeService } from './services/theme.service';
import { AuthService } from './services/auth.service';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, NavbarComponent, FooterComponent, ToastComponent],
  templateUrl: './app.html'
})
export class App implements OnInit {
  mostrarLayout = false;

  constructor(
    private router: Router,
    private themeService: ThemeService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    // Verificar al iniciar la app
    this.mostrarLayout = this.authService.isLoggedIn();

    // Verificar en cada cambio de ruta (login/logout)
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd)
    ).subscribe(() => {
      this.mostrarLayout = this.authService.isLoggedIn();
    });
  }
}
