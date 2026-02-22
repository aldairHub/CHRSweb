import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UsuarioService } from '../../services/usuario.service';

@Component({
  selector: 'app-cambiar-clave-obligatorio',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './cambiar-clave-obligatorio.html',
  styleUrls: ['./cambiar-clave-obligatorio.scss']
})
export class CambiarClaveObligatorioComponent {

  claveNueva            = '';
  claveNuevaConfirmacion = '';
  showClave             = false;
  showClaveConfirm      = false;
  isLoading             = false;
  error                 = '';
  exito                 = '';

  constructor(
    private usuarioSvc: UsuarioService,
    private authSvc:    AuthService,
    private router:     Router
  ) {}

  toggleClave():        void { this.showClave        = !this.showClave; }
  toggleClaveConfirm(): void { this.showClaveConfirm = !this.showClaveConfirm; }

  onSubmit(): void {
    this.error = '';
    this.exito = '';

    // Validaciones locales
    if (!this.claveNueva || !this.claveNuevaConfirmacion) {
      this.error = 'Completa todos los campos.';
      return;
    }

    if (this.claveNueva.length < 8) {
      this.error = 'La contraseña debe tener al menos 8 caracteres.';
      return;
    }

    if (this.claveNueva !== this.claveNuevaConfirmacion) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }

    this.isLoading = true;

    this.usuarioSvc.cambiarClavePrimerLogin(
      this.claveNueva,
      this.claveNuevaConfirmacion
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.exito = ' Contraseña actualizada correctamente.';

        //  Limpiar flag y redirigir a su dashboard
        this.authSvc.limpiarPrimerLogin();

        setTimeout(() => {
          this.authSvc.redirigirPorRol();
        }, 1500);
      },
      error: (err) => {
        this.isLoading = false;
        this.error = err?.error || 'No se pudo actualizar la contraseña. Intenta de nuevo.';
      }
    });
  }
}
