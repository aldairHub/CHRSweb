import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';

@Component({
  selector: 'app-recuperar-clave',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recuperar-clave.html',
  styleUrls: ['./recuperar-clave.scss']
})
export class RecuperarClaveComponent {

  correo    = '';
  isLoading = false;
  error     = '';
  exito     = false; // ✅ flag para mostrar pantalla de éxito

  constructor(
    private usuarioSvc: UsuarioService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  onSubmit(): void {
    this.error = '';

    if (!this.correo || !this.correo.includes('@')) {
      this.error = 'Ingresa un correo electrónico válido.';
      return;
    }

    this.isLoading = true;
    this.cdr.detectChanges();

    this.usuarioSvc.recuperarClave(this.correo).subscribe({
      next: () => {
        this.isLoading = false;
        this.exito = true;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.exito = true;
        this.cdr.detectChanges();
      }
    });
  }

  volverAlLogin(): void {
    this.router.navigate(['/login']);
  }
}
