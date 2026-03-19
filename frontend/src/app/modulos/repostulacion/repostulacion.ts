import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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
  selector: 'app-repostulacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './repostulacion.html',
  styleUrls: ['./repostulacion.scss']
})
export class RepostulacionComponent implements OnInit {

  cedula            = '';
  cedulaVerificada  = false;
  verificando       = false;
  enviando          = false;
  error             = '';
  exito             = false;
  mensajeExito      = '';

  idSolicitud: number | null = null;

  // ─── Archivos base ────────────────────────────────────────────
  archivoCedula:   File | null = null;
  archivoFoto:     File | null = null;
  nombreArchivoCedula = '';
  nombreArchivoFoto   = '';

  // ─── Documentos académicos dinámicos ─────────────────────────
  documentosAcademicos: DocumentoEntrada[] = [];

  // ─── Requisitos obligatorios de la solicitud ─────────────────
  requisitos: RequisitoPrepostulacion[] = [];

  private apiUrl = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router: Router,
    private route:  ActivatedRoute,
    private http:   HttpClient,
    private cdr:    ChangeDetectorRef
  ) {}

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

  // ==========================================
  // PASO 1 – VERIFICAR CÉDULA
  // ==========================================
  verificarCedula(): void {
    if (!this.cedula) return;
    this.verificando = true;
    this.error       = '';

    this.http.get<any>(`${this.apiUrl}/verificar-estado/${this.cedula}`).subscribe({
      next: (res) => {
        this.verificando = false;
        if (!res.encontrado) {
          this.error = 'No se encontró ninguna solicitud con esta cédula.';
        } else if (res.estado?.toUpperCase() === 'RECHAZADO') {
          this.cedulaVerificada = true;
        } else {
          this.error = `Su solicitud tiene estado "${res.estado}". Solo puede re-postular si fue rechazada.`;
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.verificando = false;
        this.error = 'Error al verificar. Intente más tarde.';
        this.cdr.detectChanges();
      }
    });
  }

  // ==========================================
  // SELECCIÓN DE ARCHIVOS BASE
  // ==========================================
  onFileCedulaSelected(e: any): void {
    const f = e.target.files[0];
    if (f?.type === 'application/pdf') {
      this.archivoCedula       = f;
      this.nombreArchivoCedula = f.name;
    } else {
      alert('Debe ser un archivo PDF');
      e.target.value = '';
    }
  }

  onFileFotoSelected(e: any): void {
    const f = e.target.files[0];
    if (f?.type.startsWith('image/')) {
      this.archivoFoto       = f;
      this.nombreArchivoFoto = f.name;
    } else {
      alert('Debe ser una imagen (JPG, PNG)');
      e.target.value = '';
    }
  }

  // ==========================================
  // GESTIÓN DE DOCUMENTOS ACADÉMICOS
  // ==========================================
  onFileDocumentoSelected(e: any, index: number): void {
    const f = e.target.files[0];
    if (f?.type === 'application/pdf') {
      this.documentosAcademicos[index].archivo       = f;
      this.documentosAcademicos[index].nombreArchivo = f.name;
    } else {
      alert('Solo se permiten archivos PDF');
      e.target.value = '';
    }
  }

  agregarDocumento(): void {
    if (this.documentosAcademicos.length >= 10) {
      alert('Máximo 10 documentos permitidos');
      return;
    }
    this.documentosAcademicos.push({ archivo: null, descripcion: '', nombreArchivo: '' });
  }

  eliminarDocumento(index: number): void {
    // Si hay requisitos obligatorios, puede llegar a 0 documentos académicos
    if (this.requisitos.length === 0 && this.documentosAcademicos.length <= 1) {
      alert('Debe existir al menos un documento académico');
      return;
    }
    this.documentosAcademicos.splice(index, 1);
  }

  // ==========================================
  // REQUISITOS OBLIGATORIOS
  // ==========================================
  cargarRequisitos(idSolicitud: number): void {
    this.http.get<RequisitoPrepostulacion[]>(
      `${this.apiUrl}/solicitud/${idSolicitud}/requisitos`
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
      this.requisitos[index].archivo       = f;
      this.requisitos[index].nombreArchivo = f.name;
    } else {
      alert('Debe ser un archivo PDF');
      event.target.value = '';
    }
    this.cdr.detectChanges();
  }

  // ==========================================
  // ENVIAR RE-POSTULACIÓN
  // ==========================================
  enviarRepostulacion(): void {
    if (!this.archivoCedula || !this.archivoFoto) {
      this.error = 'Debes subir la cédula y la foto.';
      return;
    }

    // Si NO hay requisitos obligatorios del Revisor, exige al menos un doc académico
    if (this.requisitos.length === 0 && this.documentosAcademicos.length === 0) {
      this.error = 'Debe agregar al menos un documento académico (título profesional).';
      return;
    }

    for (const doc of this.documentosAcademicos) {
      if (!doc.archivo) {
        this.error = 'Todos los documentos académicos deben tener un archivo PDF.';
        return;
      }
      if (!doc.descripcion.trim()) {
        this.error = 'Todos los documentos académicos deben tener una descripción.';
        return;
      }
    }
    for (const req of this.requisitos) {
      if (!req.archivo) {
        this.error = `Debe subir el documento requerido: "${req.nombre}"`;
        return;
      }
    }
    if (!this.idSolicitud) {
      this.error = 'No se encontró la solicitud. Vuelve a seleccionar la plaza.';
      return;
    }

    this.enviando = true;
    this.error    = '';

    const fd = new FormData();
    fd.append('cedula',        this.cedula);
    fd.append('idSolicitud',   String(this.idSolicitud));
    fd.append('archivoCedula', this.archivoCedula, this.nombreArchivoCedula);
    fd.append('archivoFoto',   this.archivoFoto,   this.nombreArchivoFoto);

    for (const doc of this.documentosAcademicos) {
      if (doc.archivo) {
        fd.append('archivosDocumentos',      doc.archivo, doc.archivo.name);
        fd.append('descripcionesDocumentos', doc.descripcion);
      }
    }

    for (const req of this.requisitos) {
      if (req.archivo) {
        fd.append('archivosRequisitos', req.archivo, req.archivo.name);
        fd.append('idsRequisitos', String(req.idRequisito));
        fd.append('nombresRequisitos', req.nombre); // ← agregar esto
      }
    }

    this.http.post<any>(`${this.apiUrl}/repostular`, fd).subscribe({
      next: (res) => {
        this.enviando     = false;
        this.exito        = true;
        this.mensajeExito = res.mensaje || 'Re-postulación enviada exitosamente.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.enviando = false;
        this.error    = err.error?.mensaje || 'Error al enviar. Intente más tarde.';
        this.cdr.detectChanges();
      }
    });
  }

  irAInicio(): void { this.router.navigate(['/']); }
}
