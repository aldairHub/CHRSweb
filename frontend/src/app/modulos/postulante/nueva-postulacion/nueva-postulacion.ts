import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';

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

export interface DocumentoReutilizable {
  idDocumento: number | null;
  tipo: 'CEDULA' | 'FOTO' | 'ACADEMICO';
  descripcion: string;
  urlDocumento: string;
  fechaSubida: string | null;
  seleccionado: boolean;
  archivoNuevo: File | null;
  nombreArchivoNuevo: string;
}

@Component({
  selector: 'app-nueva-postulacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './nueva-postulacion.html',
  styleUrls: ['./nueva-postulacion.scss']
})
export class NuevaPostulacionComponent implements OnInit {

  idSolicitud: number | null = null;
  enviando     = false;
  cargando     = false;
  error        = '';
  exito        = false;
  mensajeExito = '';

  documentosAnteriores: DocumentoReutilizable[] = [];
  // Docs académicos nuevos adicionales (mismo patrón que registro/repostulacion)
  documentosAcademicos: DocumentoEntrada[] = [];
  requisitos: RequisitoPrepostulacion[] = [];

  get cedula():     DocumentoReutilizable | null { return this.documentosAnteriores.find(d => d.tipo === 'CEDULA')    ?? null; }
  get foto():       DocumentoReutilizable | null { return this.documentosAnteriores.find(d => d.tipo === 'FOTO')      ?? null; }
  get academicos(): DocumentoReutilizable[]      { return this.documentosAnteriores.filter(d => d.tipo === 'ACADEMICO'); }

  private apiUrl            = 'http://localhost:8080/api/postulante/nueva-postulacion';
  private prepostulacionUrl = 'http://localhost:8080/api/prepostulacion';

  constructor(
    private router:      Router,
    private route:       ActivatedRoute,
    private http:        HttpClient,
    private cdr:         ChangeDetectorRef,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (!this.authService.isLoggedIn() || this.authService.getRol() !== 'postulante') {
      this.router.navigate(['/convocatorias']);
      return;
    }
    // NO se llama agregarDocumento() aquí — se hace en cargarRequisitos() igual que registro/repostulacion
    this.route.queryParams.subscribe(params => {
      if (params['idSolicitud']) {
        this.idSolicitud = +params['idSolicitud'];
        this.cargarDatos(this.idSolicitud);
      } else {
        this.router.navigate(['/convocatorias']);
      }
    });
  }

  private cargarDatos(idSolicitud: number): void {
    this.cargando = true;
    const headers = this.getHeaders();

    // Documentos anteriores
    this.http.get<DocumentoReutilizable[]>(`${this.apiUrl}/mis-documentos`, { headers }).subscribe({
      next: (docs) => {
        this.documentosAnteriores = docs.map(d => ({
          ...d, seleccionado: true, archivoNuevo: null, nombreArchivoNuevo: ''
        }));
        this.cdr.detectChanges();
      },
      error: () => { this.cdr.detectChanges(); }
    });

    // Requisitos — misma lógica que registro/repostulacion
    this.cargarRequisitos(idSolicitud);
  }

  cargarRequisitos(idSolicitud: number): void {
    this.http.get<RequisitoPrepostulacion[]>(
      `${this.prepostulacionUrl}/solicitud/${idSolicitud}/requisitos`
    ).subscribe({
      next: (data) => {
        this.requisitos = data.map(r => ({ ...r, archivo: null, nombreArchivo: '' }));
        this.cargando   = false;
        // Mismo patrón que registro/repostulacion:
        // solo agrega doc por defecto si NO hay requisitos obligatorios
        if (this.requisitos.length === 0) {
          this.agregarDocumento();
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        // Si falla la carga, agregar doc por defecto (comportamiento seguro)
        this.agregarDocumento();
        this.cdr.detectChanges();
      }
    });
  }

  // ── Docs anteriores: reutilizar o nuevo ──────────────────
  onArchivoNuevoSelected(event: any, doc: DocumentoReutilizable): void {
    const f = event.target.files[0];
    if (doc.tipo === 'FOTO') {
      if (!f?.type.startsWith('image/')) { alert('Debe ser una imagen (JPG, PNG)'); event.target.value = ''; return; }
    } else {
      if (f?.type !== 'application/pdf') { alert('Debe ser un archivo PDF'); event.target.value = ''; return; }
    }
    if (doc.seleccionado && f) {
      this.error = `No puede reutilizar "${doc.descripcion}" y subir uno nuevo. Deseleccione primero.`;
      event.target.value = ''; this.cdr.detectChanges(); return;
    }
    doc.archivoNuevo       = f;
    doc.nombreArchivoNuevo = f.name;
    this.error = ''; this.cdr.detectChanges();
  }

  toggleSeleccionado(doc: DocumentoReutilizable): void {
    if (!doc.seleccionado && doc.archivoNuevo) {
      this.error = `Elimine el archivo nuevo de "${doc.descripcion}" antes de reutilizar.`;
      this.cdr.detectChanges(); return;
    }
    doc.seleccionado = !doc.seleccionado;
    this.error = ''; this.cdr.detectChanges();
  }

  limpiarArchivoNuevo(doc: DocumentoReutilizable): void {
    doc.archivoNuevo = null; doc.nombreArchivoNuevo = ''; this.cdr.detectChanges();
  }

  // ── Docs académicos nuevos adicionales ───────────────────
  agregarDocumento(): void {
    if (this.documentosAcademicos.length >= 10) return;
    this.documentosAcademicos.push({ archivo: null, descripcion: '', nombreArchivo: '' });
  }

  eliminarDocumento(index: number): void {
    // Igual que repostulacion: bloquear ✕ solo si no hay requisitos y queda 1
    if (this.requisitos.length === 0 && this.documentosAcademicos.length <= 1) return;
    this.documentosAcademicos.splice(index, 1);
  }

  onFileDocumentoSelected(e: any, index: number): void {
    const f = e.target.files[0];
    if (f?.type === 'application/pdf') {
      this.documentosAcademicos[index].archivo      = f;
      this.documentosAcademicos[index].nombreArchivo = f.name;
    } else { alert('Solo se permiten archivos PDF'); e.target.value = ''; }
  }

  // ── Requisitos ────────────────────────────────────────────
  onFileRequisitoSelected(event: any, index: number): void {
    const f = event.target.files[0];
    if (f?.type === 'application/pdf') {
      this.requisitos[index].archivo       = f;
      this.requisitos[index].nombreArchivo  = f.name;
    } else { alert('Debe ser un archivo PDF'); event.target.value = ''; }
    this.cdr.detectChanges();
  }

  // ── Validar y enviar ──────────────────────────────────────
  enviar(): void {
    this.error = '';

    // Validar docs anteriores (conflicto o vacío)
    for (const doc of this.documentosAnteriores) {
      if (doc.seleccionado && doc.archivoNuevo) {
        this.error = `Conflicto en "${doc.descripcion}": reutilizar y subir nuevo al mismo tiempo.`; return;
      }
      if (!doc.seleccionado && !doc.archivoNuevo) {
        this.error = `Debe reutilizar o subir un nuevo archivo para "${doc.descripcion}".`; return;
      }
    }

    // Validar docs académicos nuevos — igual que repostulacion
    if (this.requisitos.length === 0 && this.documentosAcademicos.length === 0) {
      this.error = 'Debe agregar al menos un documento académico.'; return;
    }
    for (const doc of this.documentosAcademicos) {
      if (!doc.archivo) { this.error = 'Uno de los documentos académicos no tiene archivo.'; return; }
      if (!doc.descripcion.trim()) { this.error = 'Agregue una descripción a cada documento académico.'; return; }
    }

    // Validar requisitos
    for (const req of this.requisitos) {
      if (!req.archivo) { this.error = `Debe subir el documento requerido: "${req.nombre}"`; return; }
    }

    if (!this.idSolicitud) { this.error = 'No se encontró la solicitud.'; return; }

    this.enviando = true;
    const headers = this.getHeaders();
    const fd      = new FormData();
    fd.append('idSolicitud', String(this.idSolicitud));

    // Cédula
    const ced = this.cedula;
    if (ced) {
      if (ced.seleccionado) fd.append('urlCedulaReutilizada', ced.urlDocumento);
      else if (ced.archivoNuevo) fd.append('archivoCedulaNueva', ced.archivoNuevo, ced.archivoNuevo.name);
    }

    // Foto
    const fot = this.foto;
    if (fot) {
      if (fot.seleccionado) fd.append('urlFotoReutilizada', fot.urlDocumento);
      else if (fot.archivoNuevo) fd.append('archivoFotoNueva', fot.archivoNuevo, fot.archivoNuevo.name);
    }

    // Docs académicos anteriores reutilizados o reemplazados
    for (const doc of this.academicos) {
      if (doc.seleccionado && doc.idDocumento) {
        fd.append('idsDocumentosReutilizados', String(doc.idDocumento));
      } else if (!doc.seleccionado && doc.archivoNuevo) {
        fd.append('archivosDocumentos', doc.archivoNuevo, doc.archivoNuevo.name);
        fd.append('descripcionesDocumentos', doc.descripcion);
      }
    }

    // Docs académicos nuevos adicionales
    for (const doc of this.documentosAcademicos) {
      if (doc.archivo) {
        fd.append('archivosDocumentos', doc.archivo, doc.archivo.name);
        fd.append('descripcionesDocumentos', doc.descripcion);
      }
    }

    // Requisitos
    for (const req of this.requisitos) {
      if (req.archivo) {
        fd.append('archivosRequisitos', req.archivo, req.archivo.name);
        fd.append('idsRequisitos',      String(req.idRequisito));
        fd.append('nombresRequisitos',  req.nombre);
      }
    }

    this.http.post<any>(this.apiUrl, fd, { headers }).subscribe({
      next: (res) => {
        this.enviando     = false;
        this.exito        = true;
        this.mensajeExito = res.mensaje || 'Postulación enviada exitosamente.';
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.enviando = false;
        this.error    = err.error?.mensaje || 'Error al enviar. Intente más tarde.';
        this.cdr.detectChanges();
      }
    });
  }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : new HttpHeaders();
  }

  irADashboard(): void { this.router.navigate(['/postulante']); }
}
