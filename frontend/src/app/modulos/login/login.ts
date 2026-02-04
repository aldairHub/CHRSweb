import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent {
  usuarioApp: string = '';
  claveApp: string = '';

  showPassword = false;
  isLoading = false;
  serverError = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onLogin() {
    // 1. Limpieza inicial
    this.usuarioApp = this.usuarioApp.trim();
    this.serverError = '';

    // 2. Validación rápida antes de enviar
    if (this.usuarioApp.length < 3 || this.claveApp.length < 4) {
      this.serverError = 'Por favor, revise sus credenciales.';
      return;
    }

    this.isLoading = true;

    // 3. Petición directa al servicio
    this.authService.login(this.usuarioApp, this.claveApp).subscribe({
      next: (res: any) => {
        this.isLoading = false;

        // Verificamos si llegó el token correctamente
        if (res && res.token) {

          // Extraemos el rol (ej: "ADMIN") o asignamos uno por defecto
          const rolBackend = (res.roles && res.roles.length > 0) ? res.roles[0] : 'invitado';

          // Guardamos datos en sesión
          const userData = {
            ...res,
            rol: rolBackend
          };
          this.authService.guardarSesion(res);

          // Redirigimos
          this.authService.redirigirPorRol();

        } else {
          // Si el backend responde pero no hay token (caso raro)
          this.serverError = 'Credenciales no válidas.';
        }
      },
      error: (err) => {
        this.isLoading = false;

        // Manejo de errores rápido y limpio
        if (err.status === 401) {
          this.serverError = 'Usuario o contraseña incorrectos.';
        } else if (err.status === 0) {
          this.serverError = 'No hay conexión con el servidor.';
        } else {
          this.serverError = 'Ocurrió un error inesperado.';
        }
      }
    });
  }

  private executeRedirection(rol: string) {
    const rolNormalizado = rol.toLowerCase();

    const routes: { [key: string]: string } = {
      'admin': '/admin',
      'postulante': '/postulante',
      'evaluador': '/evaluador'
    };

    const target = routes[rolNormalizado];

    if (target) {
      this.router.navigate([target]);
    } else {
      this.serverError = 'Su cuenta no tiene permisos asignados.';
    }
  }

  irARegistro() {
    this.router.navigate(['/registro']);
  }
}
