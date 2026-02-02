import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class RegistroComponent {
  currentStep: number = 1;

  // Variables Paso 1 y 2
  email: string = '';
  codigoVerificacion: string = '';

  // Variables Paso 3 (Datos)
  cedula: string = '';
  nombres: string = '';
  apellidos: string = '';

  // Archivos (AHORA SON 3)
  archivoCedula: File | null = null;
  archivoFoto: File | null = null;
  archivoPrerrequisitos: File | null = null; // ✅ NUEVO

  // Nombres para mostrar
  nombreArchivoCedula: string = '';
  nombreArchivoFoto: string = '';
  nombreArchivoPrerrequisitos: string = ''; // ✅ NUEVO

  constructor(private router: Router) {}

  // --- MÉTODOS ---

  enviarCodigo(): void {
    if (!this.email || !this.validarEmail(this.email)) {
      alert('Por favor ingrese un correo válido');
      return;
    }
    this.currentStep = 2; // Instantáneo
  }

  verificarCodigo(): void {
    if (this.codigoVerificacion.length !== 6) {
      alert('El código debe tener 6 dígitos');
      return;
    }
    this.currentStep = 3; // Instantáneo
  }

  reenviarCodigo(): void {
    alert('Código reenviado a ' + this.email);
  }

  onCedulaInput(): void {
    this.cedula = this.cedula.replace(/\D/g, '');
    if (this.cedula.length === 10) {
      this.buscarPorCedula();
    }
  }

  buscarPorCedula(): void {
    this.nombres = 'Juan Carlos';
    this.apellidos = 'Pérez González';
  }

  // --- MÉTODOS DE ARCHIVOS ---

  onFileCedulaSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (file.type !== 'application/pdf') {
        alert('El documento de identidad debe ser un PDF');
        return;
      }
      this.archivoCedula = file;
      this.nombreArchivoCedula = file.name;
    }
  }

  onFileFotoSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (!file.type.startsWith('image/')) {
        alert('El archivo debe ser una imagen (JPG, PNG)');
        return;
      }
      this.archivoFoto = file;
      this.nombreArchivoFoto = file.name;
    }
  }

  // ✅ NUEVO MÉTODO PARA PRE-REQUISITOS
  onFilePrerrequisitosSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (file.type !== 'application/pdf') {
        alert('Los pre-requisitos deben estar en formato PDF');
        return;
      }
      this.archivoPrerrequisitos = file;
      this.nombreArchivoPrerrequisitos = file.name;
    }
  }

  registrar(): void {
    if (!this.validarFormulario()) return;

    // Lógica final
    alert('Solicitud enviada con éxito. Su documentación será revisada.');
    this.router.navigate(['/login']);
  }

  // --- VALIDACIONES ---

  validarEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  validarFormulario(): boolean {
    if (!this.cedula || !this.nombres) {
      alert('Complete la información de cédula');
      return false;
    }
    if (!this.archivoCedula) {
      alert('Debe subir el PDF de su cédula o pasaporte');
      return false;
    }
    if (!this.archivoFoto) {
      alert('Debe subir la foto selfie con su cédula');
      return false;
    }
    // ✅ NUEVA VALIDACIÓN
    if (!this.archivoPrerrequisitos) {
      alert('Debe subir la documentación de Pre-requisitos (Títulos, Experiencia)');
      return false;
    }
    return true;
  }

  volverPaso() { if (this.currentStep > 1) this.currentStep--; }
  irALogin() { this.router.navigate(['/login']); }
}
