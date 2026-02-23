import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';
import {
  DocumentoService,
  DocumentoBackend,
  PostulanteInfo
} from '../../../services/documento.service';

// ============================================================
// Interfaz local del componente (UI state)
// ============================================================
export interface DocumentoUI {
  // Datos del backend
  idTipoDocumento:  number;
  nombre:           string;
  descripcion:      string;
  obligatorio:      boolean;
  idDocumento:      number | null;

  // Estado UI
  archivo:          File | null;
  nombreArchivo:    string;
  estado:           'pendiente' | 'subido' | 'validado' | 'rechazado';
  observacion:      string;
  progreso:         number;
}

// ============================================================
// SubirDocumentosComponent — con integración real al backend
// ============================================================
@Component({
  selector: 'app-subir-documentos',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent],
  templateUrl: './subir-documentos.html',
  styleUrls: ['./subir-documentos.scss']
})
export class SubirDocumentosComponent implements OnInit, OnDestroy {

  // ==========================================
  // ESTADO UI
  // ==========================================
  cargandoPagina     = true;
  subiendo           = false;
  mostrarModalExito  = false;
  documentoSubiendoId: number | null = null;

  showToast   = false;
  toastType:    'success' | 'error' | 'info' = 'success';
  toastTitle  = '';
  toastMessage = '';
  private toastTimer: any;

  // ==========================================
  // DATOS DEL POSTULANTE (vienen de SP 5)
  // ==========================================
  postulante: PostulanteInfo | null = null;
  idPostulacion: number | null = null;

  // ==========================================
  // DOCUMENTOS (vienen de SP 1)
  // ==========================================
  documentos: DocumentoUI[] = [];

  // Mapeo: id_tipo_documento → descripcion (estática, puedes moverla a BD)
  private descripcionesMap: Record<string, string> = {
    // Llena esto según tus tipos de documento en BD
    // o agrega una columna 'descripcion' a tipo_documento
    'default': 'Documento requerido para el proceso de selección docente.'
  };

  // Subject para progreso de subida
  private progreso$ = new Subject<number>();

  constructor(
    private router: Router,
    private documentoSvc: DocumentoService
  ) {}

  // ==========================================
  // LIFECYCLE
  // ==========================================
  ngOnInit(): void {
    // Obtener idUsuario del token JWT (ajusta según tu AuthService)
    const idUsuario = this.obtenerIdUsuarioActual();
    if (!idUsuario) {
      this.router.navigate(['/login']);
      return;
    }
    this.cargarInfoPostulante(idUsuario);
  }

  ngOnDestroy(): void {
    clearTimeout(this.toastTimer);
    this.progreso$.complete();
  }

  // ==========================================
  // CARGA INICIAL
  // ==========================================
  private cargarInfoPostulante(idUsuario: number): void {
    this.documentoSvc.obtenerInfoPostulante(idUsuario).subscribe({
      next: info => {
        this.postulante   = info;
        this.idPostulacion = info.idPostulacion;
        this.cargarDocumentos(info.idPostulacion);
      },
      error: () => {
        this.mostrarToast('error', 'Error', 'No se pudo cargar la información del postulante.');
        this.cargandoPagina = false;
      }
    });
  }

  private cargarDocumentos(idPostulacion: number): void {
    this.documentoSvc.obtenerDocumentos(idPostulacion).subscribe({
      next: docs => {
        this.documentos = docs.map(d => this.mapearDocumento(d));
        this.cargandoPagina = false;
      },
      error: () => {
        this.mostrarToast('error', 'Error', 'No se pudo cargar la lista de documentos.');
        this.cargandoPagina = false;
      }
    });
  }

  private mapearDocumento(d: DocumentoBackend): DocumentoUI {
    return {
      idTipoDocumento:  d.idTipoDocumento,
      nombre:           d.nombreTipo,
      descripcion:      d.descripcionTipo ?? 'Documento requerido para el proceso.',
      obligatorio:      d.obligatorio,
      idDocumento:      d.idDocumento,
      archivo:          null,
      nombreArchivo:    d.rutaArchivo ? d.rutaArchivo.split('/').pop() ?? '' : '',
      estado:           (d.estadoValidacion as DocumentoUI['estado']) ?? 'pendiente',
      observacion:      d.observacionesIa ?? '',
      progreso:         d.idDocumento ? 100 : 0
    };
  }

  // ==========================================
  // GETTERS
  // ==========================================
  get totalSubidos(): number {
    return this.documentos.filter(d => d.estado === 'subido' || d.estado === 'validado').length;
  }

  get obligatoriosCompletos(): boolean {
    return this.documentos
      .filter(d => d.obligatorio)
      .every(d => d.estado === 'subido' || d.estado === 'validado');
  }

  get porcentajeCompletado(): number {
    if (!this.documentos.length) return 0;
    return Math.round((this.totalSubidos / this.documentos.length) * 100);
  }

  get nombreCompleto(): string {
    if (!this.postulante) return '';
    return `${this.postulante.nombres} ${this.postulante.apellidos}`;
  }

  get procesoLabel(): string {
    if (!this.postulante) return '';
    return `Selección Docente — ${this.postulante.nombreMateria} · ${this.postulante.nombreCarrera}`;
  }

  // ==========================================
  // SUBIR ARCHIVO
  // ==========================================
  onFileSelected(event: Event, doc: DocumentoUI): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const archivo = input.files[0];

    if (archivo.type !== 'application/pdf') {
      this.mostrarToast('error', 'Formato inválido', 'Solo se aceptan archivos en formato PDF.');
      return;
    }
    if (archivo.size > 10 * 1024 * 1024) {
      this.mostrarToast('error', 'Archivo demasiado grande', 'El tamaño máximo permitido es 10 MB.');
      return;
    }

    doc.archivo = archivo;
    doc.nombreArchivo = archivo.name;
    doc.progreso = 0;
    this.documentoSubiendoId = doc.idTipoDocumento;

    const progreso$ = new Subject<number>();
    progreso$.subscribe(pct => { doc.progreso = pct; });

    this.documentoSvc.subirDocumento(
      this.idPostulacion!,
      doc.idTipoDocumento,
      archivo,
      progreso$
    ).subscribe({
      next: res => {
        if (res.exitoso) {
          doc.estado      = 'subido';
          doc.idDocumento = res.idDocumento;
          this.mostrarToast('success', '¡Archivo cargado!', `"${doc.nombre}" subido correctamente.`);
        } else {
          doc.progreso    = 0;
          doc.archivo     = null;
          doc.nombreArchivo = '';
          this.mostrarToast('error', 'Error al subir', res.mensaje);
        }
        this.documentoSubiendoId = null;
        progreso$.complete();
      },
      error: () => {
        doc.progreso = 0;
        this.documentoSubiendoId = null;
        this.mostrarToast('error', 'Error', 'No se pudo conectar con el servidor.');
        progreso$.complete();
      }
    });
  }

  eliminarArchivo(doc: DocumentoUI): void {
    if (!doc.idDocumento || !this.idPostulacion) {
      // Si no tiene id en BD, solo limpiar UI
      doc.archivo = null; doc.nombreArchivo = ''; doc.estado = 'pendiente'; doc.progreso = 0;
      return;
    }
    this.documentoSvc.eliminarDocumento(doc.idDocumento, this.idPostulacion).subscribe({
      next: res => {
        if (res.exitoso) {
          doc.archivo = null; doc.nombreArchivo = '';
          doc.estado  = 'pendiente'; doc.progreso = 0; doc.idDocumento = null;
          this.mostrarToast('info', 'Eliminado', 'El documento fue eliminado.');
        } else {
          this.mostrarToast('error', 'No se pudo eliminar', res.mensaje);
        }
      },
      error: () => this.mostrarToast('error', 'Error', 'No se pudo conectar con el servidor.')
    });
  }

  triggerInput(idTipoDocumento: number): void {
    const input = document.getElementById(`file-input-${idTipoDocumento}`) as HTMLInputElement;
    if (input) input.click();
  }

  // ==========================================
  // FINALIZAR CARGA
  // ==========================================
  guardarYFinalizar(): void {
    if (!this.obligatoriosCompletos) {
      this.mostrarToast('error', 'Documentos incompletos',
        'Debe subir todos los documentos obligatorios (*) antes de finalizar.');
      return;
    }

    this.documentoSvc.finalizarCarga(this.idPostulacion!).subscribe({
      next: res => {
        if (res.exitoso) {
          this.mostrarModalExito = true;
        } else {
          this.mostrarToast('error', 'No se pudo finalizar', res.mensaje);
        }
      },
      error: () => this.mostrarToast('error', 'Error', 'No se pudo conectar con el servidor.')
    });
  }

  volver(): void {
    this.router.navigate(['/postulante']);
  }

  mostrarToast(tipo: 'success' | 'error' | 'info', titulo: string, mensaje: string): void {
    this.toastType = tipo; this.toastTitle = titulo; this.toastMessage = mensaje;
    this.showToast = true;
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => this.showToast = false, 3500);
  }

  // ==========================================
  // AUTH HELPER
  // Ajusta esto a tu AuthService / JWT decoder
  // ==========================================
  private obtenerIdUsuarioActual(): number | null {
    // Ejemplo: leer del localStorage si guardas el usuario ahí
    const raw = localStorage.getItem('usuario');
    if (!raw) return null;
    try {
      const user = JSON.parse(raw);
      return user.idUsuario ?? null;
    } catch {
      return null;
    }
  }
}
