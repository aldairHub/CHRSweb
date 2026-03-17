import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';

// ==========================================
// INTERFAZ PARA DOCUMENTOS ACADÉMICOS
// (igual que en prepostulacion/registro)
// ==========================================
export interface DocumentoEntrada {
  archivo: File | null;
  descripcion: string;
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

  // ─── Documentos académicos dinámicos (nueva lógica) ──────────
  documentosAcademicos: DocumentoEntrada[] = [];

  private apiUrl = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router: Router,
    private route:  ActivatedRoute,
    private http:   HttpClient,
    private cdr:    ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.agregarDocumento(); // un campo vacío por defecto

    this.route.queryParams.subscribe(params => {
      if (params['idSolicitud']) {
        this.idSolicitud = +params['idSolicitud'];
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
      this.archivoCedula     = f;
      this.nombreArchivoCedula = f.name;
    } else {
      alert('Debe ser un archivo PDF');
      e.target.value = '';
    }
  }

  onFileFotoSelected(e: any): void {
    const f = e.target.files[0];
    if (f?.type.startsWith('image/')) {
      this.archivoFoto     = f;
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
      this.documentosAcademicos[index].archivo      = f;
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
    if (this.documentosAcademicos.length <= 1) {
      alert('Debe existir al menos un documento académico');
      return;
    }
    this.documentosAcademicos.splice(index, 1);
  }

  // ==========================================
  // ENVIAR RE-POSTULACIÓN
  // ==========================================
  enviarRepostulacion(): void {
    if (!this.archivoCedula || !this.archivoFoto) {
      this.error = 'Debes subir la cédula y la foto.';
      return;
    }

    // Validar documentos académicos
    if (this.documentosAcademicos.length === 0) {
      this.error = 'Debe agregar al menos un documento académico.';
      return;
    }
    for (const doc of this.documentosAcademicos) {
      if (!doc.archivo) {
        this.error = 'Todos los documentos deben tener un archivo PDF.';
        return;
      }
      if (!doc.descripcion.trim()) {
        this.error = 'Todos los documentos deben tener una descripción.';
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
    fd.append('cedula',     this.cedula);
    fd.append('idSolicitud', String(this.idSolicitud));
    fd.append('archivoCedula', this.archivoCedula, this.nombreArchivoCedula);
    fd.append('archivoFoto',   this.archivoFoto,   this.nombreArchivoFoto);

    // Documentos académicos dinámicos
    for (const doc of this.documentosAcademicos) {
      if (doc.archivo) {
        fd.append('archivosDocumentos',       doc.archivo, doc.archivo.name);
        fd.append('descripcionesDocumentos',  doc.descripcion);
      }
    }

    this.http.post<any>(`${this.apiUrl}/repostular`, fd).subscribe({
      next: (res) => {
        this.enviando    = false;
        this.exito       = true;
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
