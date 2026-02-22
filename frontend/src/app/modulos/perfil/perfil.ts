import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../component/navbar';
import { UsuarioService } from '../../services/usuario.service';

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './perfil.html',
  styleUrls: ['./perfil.scss']
})
export class PerfilComponent {

  // ─── Datos usuario ─────────────────────────────────────────
  nombreUsuario = localStorage.getItem('usuario') || '';
  rolUsuario    = localStorage.getItem('rol') || '';

  // ─── Formulario cambio de clave ────────────────────────────
  claveActual            = '';
  claveNueva             = '';
  claveNuevaConfirmacion = '';

  showClaveActual   = false;
  showClaveNueva    = false;
  showClaveConfirm  = false;

  isLoading = false;
  error     = '';
  exito     = '';

  constructor(private usuarioSvc: UsuarioService) {}

  toggleClaveActual():  void { this.showClaveActual  = !this.showClaveActual; }
  toggleClaveNueva():   void { this.showClaveNueva   = !this.showClaveNueva; }
  toggleClaveConfirm(): void { this.showClaveConfirm = !this.showClaveConfirm; }

  onSubmit(): void {
    this.error = '';
    this.exito = '';

    // Validaciones locales
    if (!this.claveActual || !this.claveNueva || !this.claveNuevaConfirmacion) {
      this.error = 'Completa todos los campos.';
      return;
    }

    if (this.claveNueva.length < 8) {
      this.error = 'La nueva contraseña debe tener al menos 8 caracteres.';
      return;
    }

    if (this.claveNueva !== this.claveNuevaConfirmacion) {
      this.error = 'Las contraseñas nuevas no coinciden.';
      return;
    }

    if (this.claveActual === this.claveNueva) {
      this.error = 'La nueva contraseña debe ser diferente a la actual.';
      return;
    }

    this.isLoading = true;

    this.usuarioSvc.cambiarClave(
      this.claveActual,
      this.claveNueva,
      this.claveNuevaConfirmacion
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.exito = '✅ Contraseña actualizada correctamente.';
        this.limpiarFormulario();
      },
      error: (err) => {
        this.isLoading = false;
        this.error = err?.error || 'No se pudo actualizar la contraseña.';
      }
    });
  }

  private limpiarFormulario(): void {
    this.claveActual            = '';
    this.claveNueva             = '';
    this.claveNuevaConfirmacion = '';
  }
}
