import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LoginComponent } from './modulos/login/login';
import { ThemeService } from './services/theme.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.html'
})
export class App {
  // Inyectar aquí garantiza que ThemeService se inicialice
  // (lee localStorage y aplica la clase en <html>) al arrancar la app
  constructor(private themeService: ThemeService) {}
}
const routes = [
  { path: '', component: LoginComponent }
];
