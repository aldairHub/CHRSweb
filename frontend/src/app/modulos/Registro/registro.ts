import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnDestroy, ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class RegistroComponent implements OnDestroy {

  // --- VARIABLES DE ESTADO ---
  currentStep: number = 1;
  cargando: boolean = false;       // Para el spinner de botones
  enviandoCodigo: boolean = false; // Para el paso 1
  puedeReenviar: boolean = false;  // Controla si se puede clickear "Reenviar"

  // --- TEMPORIZADOR ---
  tiempoRestante: number = 60;
  private intervalId: any = null; // Variable unificada para el timer

  // --- VARIABLES DE DATOS ---
  email: string = '';
  codigoVerificacion: string = '';

  // Paso 3
  cedula: string = '';
  nombres: string = '';
  apellidos: string = '';

  // Archivos
  archivoCedula: File | null = null;
  archivoFoto: File | null = null;
  archivoPrerrequisitos: File | null = null;

  // Nombres para mostrar en los inputs de archivo
  nombreArchivoCedula: string = '';
  nombreArchivoFoto: string = '';
  nombreArchivoPrerrequisitos: string = '';

  // URL Base del Backend
  private baseUrl = 'http://localhost:8080/api/verificacion';

  constructor(
    private router: Router,
    private http: HttpClient,
    private cdr: ChangeDetectorRef // Importante para actualizar la vista manual
  ) {}

  // ==========================================
  //               PASO 1: ENVIAR
  // ==========================================

  enviarCodigo(): void {
    if (!this.email || !this.validarEmail(this.email)) {
      alert('Por favor ingrese un correo válido');
      return;
    }

    this.enviandoCodigo = true;
    const params = new HttpParams().set('correo', this.email);

    this.http.post(`${this.baseUrl}/enviar`, null, {
      params,
      responseType: 'text' // Esperamos texto plano o vacío, no JSON
    })
      .subscribe({
        next: (res) => {
          console.log('Código enviado:', res);
          this.enviandoCodigo = false;
          this.currentStep = 2;
          this.iniciarTemporizador(); // Arrancamos el reloj
          this.cdr.detectChanges();   // Actualizamos vista
        },
        error: (err) => {
          this.enviandoCodigo = false;
          console.error(err);

          // Manejo de errores falsos (si el backend devuelve texto y Angular espera JSON)
          if (err.status === 200) {
            this.currentStep = 2;
            this.iniciarTemporizador();
          } else {
            alert('Error al enviar el código. Verifique su conexión.');
          }
          this.cdr.detectChanges();
        }
      });
  }

  // ==========================================
  //               PASO 2: VERIFICAR
  // ==========================================

  verificarCodigo(): void {
    if (!this.codigoVerificacion || this.codigoVerificacion.length !== 6) {
      alert('El código debe tener 6 dígitos');
      return;
    }

    this.cargando = true;
    this.cdr.detectChanges(); // Bloqueamos botón visualmente

    const params = new HttpParams()
      .set('correo', this.email)
      .set('codigo', this.codigoVerificacion);

    this.http.post<boolean>(`${this.baseUrl}/validar`, null, { params })
      .subscribe({
        next: (esValido) => {
          this.cargando = false; // Desbloqueamos botón siempre

          if (esValido) {
            console.log('Código aceptado');
            this.detenerTemporizador();
            this.currentStep = 3;
          } else {
            alert('Código incorrecto. Inténtalo de nuevo.');
            this.codigoVerificacion = ''; // Limpiamos para reintentar
          }
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.cargando = false;
          console.error('Error backend:', err);
          alert('El código es incorrecto o expiró.');
          this.codigoVerificacion = '';
          this.cdr.detectChanges();
        }
      });
  }

  reenviarCodigo(): void {
    if (!this.puedeReenviar) return;

    // Reseteamos estados
    this.puedeReenviar = false;
    this.tiempoRestante = 60;
    this.enviandoCodigo = true;

    const params = new HttpParams().set('correo', this.email);

    this.http.post(`${this.baseUrl}/enviar`, null, { params, responseType: 'text' })
      .subscribe({
        next: () => {
          this.enviandoCodigo = false;
          alert('Nuevo código enviado a ' + this.email);
          this.iniciarTemporizador();
        },
        error: () => {
          this.enviandoCodigo = false;
          alert('Error al reenviar. Intente más tarde.');
          this.puedeReenviar = true; // Dejamos que intente de nuevo
          this.cdr.detectChanges();
        }
      });
  }

  // ==========================================
  //               TEMPORIZADOR
  // ==========================================

  iniciarTemporizador(): void {
    this.detenerTemporizador(); // Limpiamos por si acaso había uno corriendo
    this.tiempoRestante = 60;
    this.puedeReenviar = false;

    this.intervalId = setInterval(() => {
      if (this.tiempoRestante > 0) {
        this.tiempoRestante--;
        // --- ESTA ES LA LÍNEA MÁGICA PARA QUE BAJEN LOS SEGUNDOS ---
        this.cdr.detectChanges();
      } else {
        // Se acabó el tiempo
        this.puedeReenviar = true;
        this.detenerTemporizador();
        this.cdr.detectChanges();
      }
    }, 1000);
  }

  detenerTemporizador(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  ngOnDestroy(): void {
    this.detenerTemporizador();
  }

  // ==========================================
  //          PASO 3: DATOS Y ARCHIVOS
  // ==========================================

  onCedulaInput(): void {
    // Solo permitir números
    this.cedula = this.cedula.replace(/\D/g, '');
  }

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
  renombrarArchivo(tipo: string): void {
    const nuevoNombre = prompt('Ingrese el nuevo nombre del archivo (sin extensión):');
    if (!nuevoNombre) return;

    switch(tipo) {
      case 'cedula':
        if (this.archivoCedula) this.nombreArchivoCedula = nuevoNombre + '.pdf';
        break;
      case 'foto':
        if (this.archivoFoto) {
          // Mantenemos la extensión original de la imagen
          const ext = this.archivoFoto.name.split('.').pop();
          this.nombreArchivoFoto = nuevoNombre + '.' + ext;
        }
        break;
      case 'prerrequisitos':
        if (this.archivoPrerrequisitos) this.nombreArchivoPrerrequisitos = nuevoNombre + '.pdf';
        break;
    }
  }

  registrar(): void {
    if (!this.validarFormulario()) return;

    this.cargando = true;

    // AQUÍ IRÁ TU LÓGICA DE SUBIR ARCHIVOS AL BACKEND MÁS ADELANTE
    console.log('Enviando datos:', {
      cedula: this.cedula,
      nombres: this.nombres,
      archivos: [this.archivoCedula, this.archivoFoto]
    });

    setTimeout(() => {
      this.cargando = false;
      alert('Solicitud enviada con éxito. Su documentación será revisada.');
      this.router.navigate(['/login']);
    }, 2000);
  }

  // --- VALIDACIONES AUXILIARES ---

  validarEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  validarFormulario(): boolean {
    // Agrega validaciones según necesites
    if (!this.cedula) {
      alert('Ingrese su cédula');
      return false;
    }
    if (!this.archivoCedula) {
      alert('Falta subir la cédula (PDF)');
      return false;
    }
    if (!this.archivoFoto) {
      alert('Falta subir la foto');
      return false;
    }
    return false; // OJO: Cambia esto a true cuando quieras que pase
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
}
