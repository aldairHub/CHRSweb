import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { AuthStateService } from '../../services/auth-state.service';
import { LogoService } from '../../services/logo.service';
import { AsyncPipe } from '@angular/common';


@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, AsyncPipe],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent implements OnInit {
  usuarioApp: string = '';
  claveApp: string = '';
  recordarme: boolean = false;

  showPassword = false;
  isLoading = false;
  serverError: string = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private authState: AuthStateService,
    public logoService: LogoService
  ) {}

  ngOnInit() {
    // Si ya hay sesión activa en el AuthStateService, redirigir directo
    if (this.authState.isAutenticado()) {
      this.authService.redirigirPorRol();
    }
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onLogin() {
    this.serverError = '';

    if (!this.usuarioApp || !this.claveApp) {
      this.serverError = 'Por favor, ingrese usuario y contraseña.';
      return;
    }

    this.isLoading = true;

    this.authService.login(this.usuarioApp, this.claveApp).subscribe({
      next: (res: any) => {
        this.isLoading = false;

        if (res && res.token) {
          // 1) Guardar en localStorage (compatibilidad con AuthService existente)
          this.authService.guardarSesion(res, this.recordarme);

          // 2) ✅ CRÍTICO: Actualizar el AuthStateService (BehaviorSubject en memoria)
          //    Sin esto, el AuthGuard ve isAutenticado()=false y redirige de vuelta al login
          this.authState.setEstado({
            token: res.token,
            usuarioApp: res.usuarioApp,
            roles: Array.isArray(res.roles) ? Array.from(res.roles) : [],
            moduloNombre: res.modulo?.moduloNombre ?? null,
            moduloRuta:   res.modulo?.moduloRuta   ?? null,
            opciones:     res.modulo?.opciones     ?? [],
            idUsuario:    res.idUsuario             ?? null,
            nombreRolApp: res.nombreRolApp          ?? null,
          });

          // 3) Redirigir según rol
          this.authService.redirigirPorRol();

        } else {
          this.serverError = 'Error: No se recibió el token de seguridad.';
        }
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error login:', err);

        if (err.status === 401) {
          this.serverError = 'Usuario o contraseña incorrectos.';
        } else if (err.status === 0) {
          this.serverError = 'No se pudo conectar con el servidor. Revise su internet o si el backend está encendido.';
        } else {
          this.serverError = 'Ocurrió un error inesperado. Intente más tarde.';
        }
      }
    });
  }

  irAConvocatorias() {
    this.router.navigate(['/convocatorias']);
  }

  irARecuperarClave() {
    this.router.navigate(['/recuperar-clave']);
  }
  onLogoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'imgs/logo-uteq.png';
  }
}
