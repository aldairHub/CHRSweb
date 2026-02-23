import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ConvocatoriaService } from '../../services/convocatoria.service';


@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class RegistroComponent implements OnDestroy, OnInit {

  // ==========================================
  // CONVOCATORIA
  // ==========================================
  convocatoriaId: number | null = null;
  nombreConvocatoria = '';

  // ==========================================
  // ESTADO GENERAL
  // ==========================================
  currentStep = 1;
  cargando = false;
  enviandoCodigo = false;
  puedeReenviar = false;
  mostrarModalExito = false;
  mostrarModalError = false;

  tiempoRestante = 60;
  private intervalId: any = null;

  // ==========================================
  // DATOS USUARIO
  // ==========================================
  email = '';
  codigoVerificacion = '';

  tipoDocumento = 'CEDULA';
  cedula = '';
  nombres = '';
  apellidos = '';

  // ==========================================
  // ARCHIVOS
  // ==========================================
  archivoCedula: File | null = null;
  archivoFoto: File | null = null;
  archivoPrerrequisitos: File | null = null;

  nombreArchivoCedula = '';
  nombreArchivoFoto = '';
  nombreArchivoPrerrequisitos = '';

  // ==========================================
  // URLS BACKEND
  // ==========================================
  private baseUrlVerificacion = 'http://localhost:8080/api/verificacion';
  private baseUrlPrepostulacion = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router: Router,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private route: ActivatedRoute,
    private convocatoriaService: ConvocatoriaService
  ) {}

  // ==========================================
  // INIT
  // ==========================================
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['convocatoriaId']) {
        this.convocatoriaId = +params['convocatoriaId'];
        this.convocatoriaService.obtener(this.convocatoriaId).subscribe({
          next: (conv) => { this.nombreConvocatoria = conv.titulo; },
          error: () => {}
        });
      } else {
        this.router.navigate(['/convocatorias']);
      }
    });
  }

  ngOnDestroy(): void {
    this.detenerTemporizador();
  }

  // ==========================================
  // PASO 1 - ENVIAR CODIGO
  // ==========================================
  enviarCodigo(): void {
    if (!this.validarEmail(this.email)) {
      alert('Ingrese un correo válido');
      return;
    }

    this.enviandoCodigo = true;

    const params = new HttpParams().set('correo', this.email);

    this.http.post(`${this.baseUrlVerificacion}/enviar`, null, {
      params,
      responseType: 'text'
    }).subscribe({
      next: () => {
        this.enviandoCodigo = false;
        this.currentStep = 2;
        this.iniciarTemporizador();
        this.cdr.detectChanges();
      },
      error: () => {
        this.enviandoCodigo = false;
        alert('Error al enviar el código');
        this.cdr.detectChanges();
      }
    });
  }

  // Alias que usa el HTML en el modal de error y en el paso 2
  reenviarCodigo(): void {
    this.enviarCodigo();
  }

  // ==========================================
  // PASO 2 - VALIDAR CODIGO
  // ==========================================
  verificarCodigo(): void {
    if (this.codigoVerificacion.length !== 6) {
      alert('El código debe tener 6 dígitos');
      return;
    }

    this.cargando = true;

    const params = new HttpParams()
      .set('correo', this.email)
      .set('codigo', this.codigoVerificacion);

    this.http.post<boolean>(`${this.baseUrlVerificacion}/validar`, null, { params })
      .subscribe({
        next: (valido) => {
          this.cargando = false;
          if (valido) {
            this.detenerTemporizador();
            this.currentStep = 3;
          } else {
            this.mostrarModalError = true;
            this.codigoVerificacion = '';
          }
          this.cdr.detectChanges();
        },
        error: () => {
          this.cargando = false;
          this.mostrarModalError = true;
          this.codigoVerificacion = '';
          this.cdr.detectChanges();
        }
      });
  }

  // ==========================================
  // NAVEGACIÓN ENTRE PASOS
  // ==========================================
  volverPaso(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      this.detenerTemporizador();
      this.cdr.detectChanges();
    }
  }

  // ==========================================
  // TEMPORIZADOR
  // ==========================================
  iniciarTemporizador(): void {
    this.detenerTemporizador();
    this.tiempoRestante = 60;
    this.puedeReenviar = false;

    this.intervalId = setInterval(() => {
      if (this.tiempoRestante > 0) {
        this.tiempoRestante--;
      } else {
        this.puedeReenviar = true;
        this.detenerTemporizador();
      }
      this.cdr.detectChanges();
    }, 1000);
  }

  detenerTemporizador(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  // ==========================================
  // INPUT CÉDULA
  // ==========================================
  onCedulaInput(): void {
    if (this.tipoDocumento === 'CEDULA') {
      this.cedula = this.cedula.replace(/[^0-9]/g, '');
    }
  }

  // ==========================================
  // SELECCIÓN DE ARCHIVOS
  // ==========================================
  onFileCedulaSelected(event: any): void {
    const f = event.target.files[0];
    if (f?.type === 'application/pdf') {
      this.archivoCedula = f;
      this.nombreArchivoCedula = f.name;
    } else {
      alert('Debe ser un archivo PDF');
      event.target.value = '';
    }
  }

  onFileFotoSelected(event: any): void {
    const f = event.target.files[0];
    if (f?.type.startsWith('image/')) {
      this.archivoFoto = f;
      this.nombreArchivoFoto = f.name;
    } else {
      alert('Debe ser una imagen (JPG, PNG)');
      event.target.value = '';
    }
  }

  onFilePrerrequisitosSelected(event: any): void {
    const f = event.target.files[0];
    if (f?.type === 'application/pdf') {
      this.archivoPrerrequisitos = f;
      this.nombreArchivoPrerrequisitos = f.name;
    } else {
      alert('Debe ser un archivo PDF');
      event.target.value = '';
    }
  }

  // ==========================================
  // RENOMBRAR ARCHIVO
  // ==========================================
  renombrarArchivo(tipo: string): void {
    const nuevoNombre = prompt('Ingrese el nuevo nombre (sin extensión):');
    if (!nuevoNombre?.trim()) return;

    if (tipo === 'cedula' && this.archivoCedula) {
      const ext = this.archivoCedula.name.split('.').pop();
      this.nombreArchivoCedula = `${nuevoNombre.trim()}.${ext}`;
    } else if (tipo === 'foto' && this.archivoFoto) {
      const ext = this.archivoFoto.name.split('.').pop();
      this.nombreArchivoFoto = `${nuevoNombre.trim()}.${ext}`;
    } else if (tipo === 'prerrequisitos' && this.archivoPrerrequisitos) {
      const ext = this.archivoPrerrequisitos.name.split('.').pop();
      this.nombreArchivoPrerrequisitos = `${nuevoNombre.trim()}.${ext}`;
    }
  }

  // ==========================================
  // REGISTRAR
  // ==========================================
  registrar(): void {
    if (!this.validarFormulario()) return;

    this.cargando = true;

    const formData = new FormData();
    formData.append('correo', this.email);
    formData.append('cedula', this.cedula);
    formData.append('nombres', this.nombres);
    formData.append('apellidos', this.apellidos);

    if (this.convocatoriaId) {
      formData.append('idConvocatoria', String(this.convocatoriaId));
    }

    if (this.archivoCedula)
      formData.append('archivoCedula', this.archivoCedula, this.nombreArchivoCedula);
    if (this.archivoFoto)
      formData.append('archivoFoto', this.archivoFoto, this.nombreArchivoFoto);
    if (this.archivoPrerrequisitos)
      formData.append('archivoPrerrequisitos', this.archivoPrerrequisitos, this.nombreArchivoPrerrequisitos);

    this.http.post(`${this.baseUrlPrepostulacion}`, formData).subscribe({
      next: () => {
        this.cargando = false;
        this.mostrarModalExito = true;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        alert('Error al registrar');
        this.cdr.detectChanges();
      }
    });
  }

  // ==========================================
  // VALIDACIONES
  // ==========================================
  validarEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }

  validarFormulario(): boolean {
    if (!this.nombres || !this.apellidos || !this.cedula) {
      alert('Complete todos los campos');
      return false;
    }
    if (!this.archivoCedula || !this.archivoFoto || !this.archivoPrerrequisitos) {
      alert('Suba todos los archivos requeridos');
      return false;
    }
    return true;
  }

  // ==========================================
  // NAVEGACIÓN
  // ==========================================
  irALogin(): void {
    this.router.navigate(['/login']);
  }

  // ==========================================
  // MODALES
  // ==========================================
  cerrarModalExito(): void {
    this.mostrarModalExito = false;
    this.router.navigate(['/login']);
  }

  cerrarModalError(): void {
    this.mostrarModalError = false;
  }
}
