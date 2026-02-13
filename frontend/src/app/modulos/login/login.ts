import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { OnInit } from '@angular/core';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html', // Asegúrate que el nombre coincida con tu archivo
  styleUrls: ['./login.scss']
})
export class LoginComponent  implements OnInit{
  usuarioApp: string = '';
  claveApp: string = '';
  recordarme: boolean = false;

  showPassword = false;
  isLoading = false;
  serverError: string = '';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}


  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  onLogin() {
    // 1. Limpieza inicial
    this.serverError = '';

    // Validación simple
    if (!this.usuarioApp || !this.claveApp) {
      this.serverError = 'Por favor, ingrese usuario y contraseña.';
      return;
    }

    this.isLoading = true;
    console.log('Intentando login con:', this.usuarioApp); // Debug

    // 2. Petición al servicio
    // NOTA: Asegúrate que tu authService.login acepte (usuario, clave) y no un objeto {usuario, clave}
    this.authService.login(this.usuarioApp, this.claveApp).subscribe({
      next: (res: any) => {
        this.isLoading = false;
        console.log('Login exitoso:', res);

        if (res && res.token) {
          // Guardar sesión
          this.authService.guardarSesion(res, this.recordarme);


          // Redirigir usando la lógica del servicio o manual
          // Si tu authService tiene 'redirigirPorRol', úsalo:
          this.authService.redirigirPorRol();

          // O si prefieres hacerlo manual aquí:
          // this.executeRedirection(res.roles ? res.roles[0] : 'invitado');
        } else {
          this.serverError = 'Error: No se recibió el token de seguridad.';
        }
      },
      error: (err) => {
        this.isLoading = false; // IMPORTANTE: Apagamos el spinner
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
  ngOnInit() {
    const token = localStorage.getItem('token');
    if (token) {
      this.router.navigate(['/'], { replaceUrl: true });
    }
  }


  irARegistro() {
    this.router.navigate(['/registro'], {replaceUrl: true});
  }
}
