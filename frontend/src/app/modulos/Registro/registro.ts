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
  tipoDocumento: string = 'CEDULA'; // Por defecto Cédula
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

  // URLs Base del Backend
  private baseUrlVerificacion = 'http://localhost:8080/api/verificacion';
  private baseUrlPrepostulacion = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router: Router,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
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

    this.http.post(`${this.baseUrlVerificacion}/enviar`, null, {
      params,
      responseType: 'text'
    })
      .subscribe({
        next: (res) => {
          console.log('Código enviado:', res);
          this.enviandoCodigo = false;
          this.currentStep = 2;
          this.iniciarTemporizador();
          this.cdr.detectChanges();
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
    this.cdr.detectChanges();

    const params = new HttpParams()
      .set('correo', this.email)
      .set('codigo', this.codigoVerificacion);

    this.http.post<boolean>(`${this.baseUrlVerificacion}/validar`, null, { params })
      .subscribe({
        next: (esValido) => {
          this.cargando = false;

          if (esValido) {
            console.log('Código aceptado');
            this.detenerTemporizador();
            this.currentStep = 3;
          } else {
            alert('Código incorrecto. Inténtalo de nuevo.');
            this.codigoVerificacion = '';
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

    this.puedeReenviar = false;
    this.tiempoRestante = 60;
    this.enviandoCodigo = true;

    const params = new HttpParams().set('correo', this.email);

    this.http.post(`${this.baseUrlVerificacion}/enviar`, null, { params, responseType: 'text' })
      .subscribe({
        next: () => {
          this.enviandoCodigo = false;
          alert('Nuevo código enviado a ' + this.email);
          this.iniciarTemporizador();
        },
        error: () => {
          this.enviandoCodigo = false;
          alert('Error al reenviar. Intente más tarde.');
          this.puedeReenviar = true;
          this.cdr.detectChanges();
        }
      });
  }

  // ==========================================
  //               TEMPORIZADOR
  // ==========================================

  iniciarTemporizador(): void {
    this.detenerTemporizador();
    this.tiempoRestante = 60;
    this.puedeReenviar = false;

    this.intervalId = setInterval(() => {
      if (this.tiempoRestante > 0) {
        this.tiempoRestante--;
        this.cdr.detectChanges();
      } else {
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

  // --- LÓGICA DE VALIDACIÓN DE CÉDULA MEJORADA ---
  onCedulaInput(): void {
    if (this.tipoDocumento === 'CEDULA') {
      // 1. Solo números
      this.cedula = this.cedula.replace(/\D/g, '');

      // 2. Máximo 10 caracteres
      if (this.cedula.length > 10) {
        this.cedula = this.cedula.slice(0, 10);
      }

      // 3. Validación Matemática al completar 10 dígitos
      if (this.cedula.length === 10) {
        if (!this.validadorDeCedula(this.cedula)) {
          alert('Número de cédula inválido. Por favor verifique.');
          this.cedula = ''; // Borramos para que corrija
        }
      }

    } else {
      // CASO PASAPORTE
      // Permitir letras y números, convertir a mayúsculas
      this.cedula = this.cedula.toUpperCase().replace(/[^A-Z0-9]/g, '');

      if (this.cedula.length > 20) {
        this.cedula = this.cedula.slice(0, 20);
      }
    }
  }

  // --- ALGORITMO MÓDULO 10 ECUADOR ---
  validadorDeCedula(cedula: string): boolean {
    if (cedula.length !== 10) return false;
    const provincia = parseInt(cedula.substring(0, 2), 10);
    if (provincia < 1 || provincia > 24) return false;
    const tercerDigito = parseInt(cedula.substring(2, 3), 10);
    if (tercerDigito >= 6) return false;

    const coeficientes = [2, 1, 2, 1, 2, 1, 2, 1, 2];
    let suma = 0;

    for (let i = 0; i < 9; i++) {
      let valor = parseInt(cedula[i], 10) * coeficientes[i];
      if (valor >= 10) valor -= 9;
      suma += valor;
    }

    const decenaSuperior = Math.ceil(suma / 10) * 10;
    let digitoCalculado = decenaSuperior - suma;
    if (digitoCalculado === 10) digitoCalculado = 0;
    const ultimoDigito = parseInt(cedula[9], 10);
    return digitoCalculado === ultimoDigito;
  }

  // --- MANEJO DE ARCHIVOS ---
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
          const ext = this.archivoFoto.name.split('.').pop();
          this.nombreArchivoFoto = nuevoNombre + '.' + ext;
        }
        break;
      case 'prerrequisitos':
        if (this.archivoPrerrequisitos) this.nombreArchivoPrerrequisitos = nuevoNombre + '.pdf';
        break;
    }
  }

  // ==========================================
  //     MÉTODO REGISTRAR - INTEGRADO AL BACKEND
  // ==========================================

  registrar(): void {
    if (!this.validarFormulario()) return;

    this.cargando = true;
    this.cdr.detectChanges();

    // Crear FormData para enviar archivos
    const formData = new FormData();

    // Agregar datos del usuario
    formData.append('correo', this.email);
    formData.append('cedula', this.cedula);
    formData.append('nombres', this.nombres);
    formData.append('apellidos', this.apellidos);

    //  Nombres que coinciden exactamente con el @RequestParam del backend
    if (this.archivoCedula) {
      formData.append('archivoCedula', this.archivoCedula, this.nombreArchivoCedula);
    }
    if (this.archivoFoto) {
      formData.append('archivoFoto', this.archivoFoto, this.nombreArchivoFoto);
    }
    if (this.archivoPrerrequisitos) {
      formData.append('archivoPrerrequisitos', this.archivoPrerrequisitos, this.nombreArchivoPrerrequisitos);
    }

    // Logging para debug
    console.log('Enviando datos:', {
      tipo: this.tipoDocumento,
      cedula: this.cedula,
      nombres: this.nombres,
      apellidos: this.apellidos,
      email: this.email,
      archivos: {
        cedula: this.nombreArchivoCedula,
        foto: this.nombreArchivoFoto,
        prerrequisitos: this.nombreArchivoPrerrequisitos
      }
    });

    // ✅ POST a /api/prepostulacion
    this.http.post(`${this.baseUrlPrepostulacion}`, formData)
      .subscribe({
        next: (respuesta: any) => {
          this.cargando = false;
          console.log('✅ Registro exitoso:', respuesta);

          // Mostrar mensaje del backend si existe
          const mensaje = respuesta.mensaje || '¡Solicitud enviada con éxito!';
          alert(`${mensaje}\n\nSu documentación será revisada en breve.`);

          // Redirigir al login
          this.router.navigate(['/login']);
        },
        error: (err) => {
          this.cargando = false;
          console.error('❌ Error al registrar:', err);

          // Mostrar mensaje específico del backend si existe
          if (err.error && err.error.mensaje) {
            alert(`Error: ${err.error.mensaje}`);
          } else if (err.status === 400) {
            alert('Error: Datos inválidos. Verifique la información ingresada.');
          } else if (err.status === 500) {
            alert('Error interno del servidor. Por favor intente más tarde.');
          } else {
            alert('Error al enviar la solicitud. Por favor intente nuevamente.');
          }

          this.cdr.detectChanges();
        }
      });
  }

  // --- VALIDACIONES AUXILIARES ---

  validarEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  validarFormulario(): boolean {
    // Validar nombres
    if (!this.nombres || this.nombres.trim() === '') {
      alert('Ingrese sus nombres');
      return false;
    }

    // Validar apellidos
    if (!this.apellidos || this.apellidos.trim() === '') {
      alert('Ingrese sus apellidos');
      return false;
    }

    // Validar identificación
    if (!this.cedula) {
      alert('Ingrese su número de identificación');
      return false;
    }

    // Validación específica para cédula ecuatoriana
    if (this.tipoDocumento === 'CEDULA') {
      if (this.cedula.length !== 10) {
        alert('La cédula debe tener 10 dígitos');
        return false;
      }
      if (!this.validadorDeCedula(this.cedula)) {
        alert('Número de cédula inválido');
        return false;
      }
    }

    // Validar archivos obligatorios
    if (!this.archivoCedula) {
      alert('Falta subir el documento de identidad (PDF)');
      return false;
    }
    if (!this.archivoFoto) {
      alert('Falta subir la foto');
      return false;
    }
    if (!this.archivoPrerrequisitos) {
      alert('Falta subir los pre-requisitos (PDF)');
      return false;
    }

    return true; // ✅ Todo válido
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
