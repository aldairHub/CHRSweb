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
  // 1. Limpieza de datos: Aseguramos strings vacíos para evitar nulos
  usuarioApp: string = '';
  claveApp: string = '';

  // Variables de estado y control
  showPassword = false;
  isLoading = false;
  serverError = '';

  // Mensajes de validación en tiempo real
  userTouched = false;
  passTouched = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  // --- VALIDACIONES DE NEGOCIO ---

  /**
   * Valida el formato del usuario.
   * Previene espacios en blanco y requiere longitud mínima.
   */
  isUsuarioValido(): boolean {
    const usuarioClean = this.usuarioApp.trim();
    return usuarioClean.length >= 3 && usuarioClean.length <= 20;
  }

  /**
   * Valida la integridad de la contraseña.
   * Verifica longitud mínima para evitar peticiones innecesarias al server.
   */
  isClaveValida(): boolean {
    return this.claveApp.length >= 4;
  }

  /**
   * Validador maestro para el botón de envío.
   * Bloquea el botón si hay campos inválidos o una petición en curso.
   */
  canSubmit(): boolean {
    return this.isUsuarioValido() && this.isClaveValida() && !this.isLoading;
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onLogin() {
    // 1. Sanitización de entradas
    this.usuarioApp = this.usuarioApp.trim();
    this.serverError = '';

    // 2. Validación de Pre-vuelo (Pre-flight validation)
    if (!this.canSubmit()) {
      this.serverError = 'Por favor, revise que los datos ingresados sean correctos.';
      return;
    }

    this.isLoading = true;

    // 3. Control de Timeout (Seguridad de red)
    const timeoutId = setTimeout(() => {
      if (this.isLoading) {
        this.isLoading = false;
        this.serverError = 'Tiempo de espera agotado. Verifique su conexión.';
      }
    }, 10000); // 10 segundos de margen

    this.authService.login(this.usuarioApp, this.claveApp).subscribe({
      next: (res: any) => {
        clearTimeout(timeoutId);
        this.isLoading = false;

        // 4. Validación de Integridad de Respuesta del Backend
        if (res && res.success) {
          console.log('Login verificado correctamente');

          // 5. Normalización de Datos (Parche para campos null en BD)
          const userData = {
            ...res,
            rol: res.rol || this.mapRolIdToName(res.id_rol)
          };

          // 6. Persistencia de Sesión Segura
          this.authService.guardarSesion(userData);

          // 7. Redirección Controlada por Roles
          this.executeRedirection(res.id_rol);

        } else {
          // Error controlado por el Backend (ej: Credenciales incorrectas)
          this.serverError = res?.error || 'No se pudo validar el acceso.';
        }
      },
      error: (err) => {
        this.isLoading = false;
        clearTimeout(timeoutId);
        this.handleHttpErrors(err);
      }
    });
  }

  /**
   * Mapea IDs de rol a nombres internos para el AuthGuard.
   */
  private mapRolIdToName(id: number): string {
    const roles: { [key: number]: string } = {
      3: 'admin',
      2: 'postulante',
      1: 'evaluador'
    };
    return roles[id] || 'invitado';
  }

  /**
   * Ejecuta la navegación basada en el ID de rol.
   */
  private executeRedirection(idRol: number) {
    const routes: { [key: number]: string } = {
      3: '/admin',
      2: '/postulante',
      1: '/evaluador'
    };

    const target = routes[idRol];
    if (target) {
      this.router.navigate([target]).catch(() => {
        this.serverError = 'Error al intentar acceder al módulo solicitado.';
      });
    } else {
      this.serverError = 'Su cuenta no tiene un módulo asignado.';
    }
  }

  /**
   * Manejador de excepciones de red y servidor.
   */
  private handleHttpErrors(err: any) {
    console.error('Error capturado:', err);
    if (err.status === 0) {
      this.serverError = 'Servidor inaccesible. Verifique que el backend esté activo.';
    } else if (err.status === 401) {
      this.serverError = 'Credenciales inválidas.';
    } else if (err.status === 500) {
      this.serverError = 'Error crítico en el servidor. Contacte a soporte.';
    } else {
      this.serverError = 'Ocurrió un error inesperado al iniciar sesión.';
    }
  }
  irARegistro() {
    console.log("Intentando ir al registro..."); // Para ver en consola si funciona el clic
    this.router.navigate(['/registro']);
  }
}
