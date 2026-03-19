import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

export interface DocumentoEntrada {
  archivo: File | null;
  descripcion: string;
  nombreArchivo: string;
}

export interface RequisitoPrepostulacion {
  idRequisito: number;
  nombre: string;
  descripcion: string | null;
  orden: number;
  archivo: File | null;
  nombreArchivo: string;
}

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './registro.html',
  styleUrls: ['./registro.scss']
})
export class RegistroComponent implements OnDestroy, OnInit {

  // ==========================================
  // SOLICITUD
  // ==========================================
  idSolicitud: number | null = null;

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
  documentosAcademicos: DocumentoEntrada[] = [];

  nombreArchivoCedula = '';
  nombreArchivoFoto = '';

  // ==========================================
  // REQUISITOS OBLIGATORIOS
  // ==========================================
  requisitos: RequisitoPrepostulacion[] = [];

  // ==========================================
  // URLS BACKEND
  // ==========================================
  private baseUrlVerificacion   = 'http://localhost:8080/api/verificacion';
  private baseUrlPrepostulacion = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router: Router,
    private http:   HttpClient,
    private cdr:    ChangeDetectorRef,
    private route:  ActivatedRoute
  ) {}

  // ==========================================
  // INIT
  // ==========================================
  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['idSolicitud']) {
        this.idSolicitud = +params['idSolicitud'];
        // Cargamos requisitos primero; si no hay, agregamos 1 doc académico por defecto
        this.cargarRequisitos(this.idSolicitud);
      } else {
        this.router.navigate(['/convocatorias']);
      }
    });
  }

  ngOnDestroy(): void {
    this.detenerTemporizador();
  }

  // ==========================================
  // REQUISITOS
  // ==========================================
  cargarRequisitos(idSolicitud: number): void {
    this.http.get<RequisitoPrepostulacion[]>(
      `${this.baseUrlPrepostulacion}/solicitud/${idSolicitud}/requisitos`
    ).subscribe({
      next: (data) => {
        this.requisitos = data.map(r => ({ ...r, archivo: null, nombreArchivo: '' }));
        // Si no hay requisitos obligatorios, iniciar con 1 doc académico por defecto
        if (this.requisitos.length === 0) {
          this.agregarDocumento();
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.requisitos = [];
        // Si falla la carga, asumir que no hay requisitos e iniciar con 1 doc
        this.agregarDocumento();
        this.cdr.detectChanges();
      }
    });
  }

  onFileRequisitoSelected(event: any, index: number): void {
    const f = event.target.files[0];
    if (f?.type === 'application/pdf') {
      this.requisitos[index].archivo      = f;
      this.requisitos[index].nombreArchivo = f.name;
    } else {
      alert('Debe ser un archivo PDF');
      event.target.value = '';
    }
    this.cdr.detectChanges();
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
      params, responseType: 'text'
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

  reenviarCodigo(): void { this.enviarCodigo(); }

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
  // NAVEGACIÓN
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

  onFileDocumentoSelected(event: any, index: number): void {
    const f = event.target.files[0];
    if (f?.type === 'application/pdf') {
      this.documentosAcademicos[index].archivo = f;
      this.documentosAcademicos[index].nombreArchivo = f.name;
    } else {
      alert('Solo se permiten archivos PDF');
      event.target.value = '';
    }
  }

  // ==========================================
  // GESTIÓN DE DOCUMENTOS ACADÉMICOS
  // ==========================================
  agregarDocumento(): void {
    if (this.documentosAcademicos.length >= 10) {
      alert('Máximo 10 documentos permitidos');
      return;
    }
    this.documentosAcademicos.push({ archivo: null, descripcion: '', nombreArchivo: '' });
  }

  eliminarDocumento(index: number): void {
    if (this.documentosAcademicos.length <= 1) {
      alert('Debe existir al menos un documento académico');
      return;
    }
    this.documentosAcademicos.splice(index, 1);
  }

  renombrarArchivo(tipo: string): void {
    const nuevoNombre = prompt('Ingrese el nuevo nombre (sin extensión):');
    if (!nuevoNombre?.trim()) return;
    if (tipo === 'cedula' && this.archivoCedula) {
      const ext = this.archivoCedula.name.split('.').pop();
      this.nombreArchivoCedula = `${nuevoNombre.trim()}.${ext}`;
    } else if (tipo === 'foto' && this.archivoFoto) {
      const ext = this.archivoFoto.name.split('.').pop();
      this.nombreArchivoFoto = `${nuevoNombre.trim()}.${ext}`;
    }
  }

  // ==========================================
  // REGISTRAR
  // ==========================================
  registrar(): void {
    if (!this.validarFormulario()) return;

    this.cargando = true;

    const formData = new FormData();
    formData.append('correo',    this.email);
    formData.append('cedula',    this.cedula);
    formData.append('nombres',   this.nombres);
    formData.append('apellidos', this.apellidos);

    if (this.idSolicitud)
      formData.append('idSolicitud', String(this.idSolicitud));

    if (this.archivoCedula)
      formData.append('archivoCedula', this.archivoCedula, this.nombreArchivoCedula);
    if (this.archivoFoto)
      formData.append('archivoFoto', this.archivoFoto, this.nombreArchivoFoto);

    for (const doc of this.documentosAcademicos) {
      if (doc.archivo) {
        formData.append('archivosDocumentos', doc.archivo, doc.archivo.name);
        formData.append('descripcionesDocumentos', doc.descripcion);
      }
    }

    for (const req of this.requisitos) {
      if (req.archivo) {
        formData.append('archivosRequisitos', req.archivo, req.archivo.name);
        formData.append('idsRequisitos', String(req.idRequisito));
        formData.append('nombresRequisitos', req.nombre); // ← agregar esto
      }
    }

    this.http.post(`${this.baseUrlPrepostulacion}`, formData).subscribe({
      next: () => {
        this.cargando = false;
        this.mostrarModalExito = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        alert(err.error?.mensaje || 'Error al registrar. Intente más tarde.');
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
    if (!this.archivoCedula || !this.archivoFoto) {
      alert('Suba todos los archivos requeridos');
      return false;
    }

    // Si NO hay requisitos obligatorios del Revisor, el postulante
    // debe subir al menos un documento académico propio
    if (this.requisitos.length === 0 && this.documentosAcademicos.length === 0) {
      alert('Debe agregar al menos un documento académico (título profesional)');
      return false;
    }

    // Validar que cada documento académico agregado tenga archivo y descripción
    for (const doc of this.documentosAcademicos) {
      if (!doc.archivo) {
        alert('Todos los documentos académicos deben tener archivo PDF');
        return false;
      }
      if (!doc.descripcion.trim()) {
        alert('Todos los documentos académicos deben tener descripción');
        return false;
      }
    }

    // Validar requisitos obligatorios del Revisor
    for (const req of this.requisitos) {
      if (!req.archivo) {
        alert(`Debe subir el documento requerido: "${req.nombre}"`);
        return false;
      }
    }
    return true;
  }

  // ==========================================
  // MODALES / NAVEGACIÓN
  // ==========================================
  irALogin(): void { this.router.navigate(['/login']); }

  cerrarModalExito(): void {
    this.mostrarModalExito = false;
    this.router.navigate(['/login']);
  }

  cerrarModalError(): void { this.mostrarModalError = false; }
}
