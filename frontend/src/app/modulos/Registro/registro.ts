import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class RegistroComponent implements OnDestroy {
  currentStep: number = 1;

  // ✅ NUEVAS PROPIEDADES FALTANTES
  cargando: boolean = false;
  enviandoCodigo: boolean = false;
  puedeReenviar: boolean = false;
  tiempoRestante: number = 60;
  private intervalId: any;

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
  archivoPrerrequisitos: File | null = null;

  // Nombres para mostrar
  nombreArchivoCedula: string = '';
  nombreArchivoFoto: string = '';
  nombreArchivoPrerrequisitos: string = '';

  constructor(private router: Router, private http: HttpClient) {}

  // --- MÉTODOS ---

  enviarCodigo(): void {
    if (!this.email || !this.validarEmail(this.email)) {
      alert('Por favor ingrese un correo válido');
      return;
    }

    this.enviandoCodigo = true;

    const params = new HttpParams().set('correo', this.email);

    this.http
      .post('http://localhost:8080/api/verificacion/enviar', null, { params })
      .pipe(finalize(() => (this.enviandoCodigo = false)))
      .subscribe({
        next: () => {
          this.currentStep = 2;
          this.iniciarTemporizador();
          alert('Código enviado a ' + this.email);
        },
        error: (err) => {
          // Aquí ya no se queda cargando por finalize()
          const msg = err?.error ? String(err.error) : 'No se pudo enviar el código';
          alert(msg);
        }
      });
  }


  verificarCodigo(): void {
    if (this.codigoVerificacion.length !== 6) {
      alert('El código debe tener 6 dígitos');
      return;
    }

    this.cargando = true;

    // Simular verificación
    setTimeout(() => {
      this.cargando = false;
      this.currentStep = 3;
      this.detenerTemporizador();
      alert('Código verificado correctamente');
    }, 1000);
  }

  reenviarCodigo(): void {
    if (!this.puedeReenviar) return;

    this.enviandoCodigo = true;

    setTimeout(() => {
      this.enviandoCodigo = false;
      this.puedeReenviar = false;
      this.tiempoRestante = 60;
      this.iniciarTemporizador();
      alert('Código reenviado a ' + this.email);
    }, 1000);
  }

  // TEMPORIZADOR PARA REENVÍO
  iniciarTemporizador(): void {
    this.puedeReenviar = false;
    this.tiempoRestante = 60;

    this.intervalId = setInterval(() => {
      this.tiempoRestante--;

      if (this.tiempoRestante <= 0) {
        this.puedeReenviar = true;
        this.detenerTemporizador();
      }
    }, 1000);
  }

  detenerTemporizador(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  onCedulaInput(): void {
    this.cedula = this.cedula.replace(/\D/g, '');
    if (this.cedula.length === 10) {
      this.buscarPorCedula();
    }
  }

  buscarPorCedula(): void {
    this.cargando = true;

    setTimeout(() => {
      this.nombres = 'Juan Carlos';
      this.apellidos = 'Pérez González';
      this.cargando = false;
    }, 500);
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

  // ✅ MÉTODO RENOMBRAR ARCHIVO
  renombrarArchivo(tipo: string): void {
    const nuevoNombre = prompt('Ingrese el nuevo nombre del archivo (sin extensión):');
    if (!nuevoNombre) return;

    switch(tipo) {
      case 'cedula':
        if (this.archivoCedula) {
          this.nombreArchivoCedula = nuevoNombre + '.pdf';
        }
        break;
      case 'foto':
        if (this.archivoFoto) {
          const extension = this.archivoFoto.name.split('.').pop();
          this.nombreArchivoFoto = nuevoNombre + '.' + extension;
        }
        break;
      case 'prerrequisitos':
        if (this.archivoPrerrequisitos) {
          this.nombreArchivoPrerrequisitos = nuevoNombre + '.pdf';
        }
        break;
    }
  }

  registrar(): void {
    if (!this.validarFormulario()) return;

    this.cargando = true;

    // Simular envío al backend
    setTimeout(() => {
      this.cargando = false;
      alert('Solicitud enviada con éxito. Su documentación será revisada.');
      this.router.navigate(['/login']);
    }, 2000);
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
    if (!this.archivoPrerrequisitos) {
      alert('Debe subir la documentación de Pre-requisitos (Títulos, Experiencia)');
      return false;
    }
    return true;
  }

  volverPaso(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      if (this.currentStep === 1) {
        this.detenerTemporizador();
      }
    }
  }

  irALogin(): void {
    this.router.navigate(['/login']);
  }

  // ✅ LIMPIAR TEMPORIZADOR AL DESTRUIR COMPONENTE
  ngOnDestroy(): void {
    this.detenerTemporizador();
  }
}
