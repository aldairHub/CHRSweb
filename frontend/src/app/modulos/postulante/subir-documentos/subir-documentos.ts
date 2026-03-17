import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';
import {
  DocumentoService,
  DocumentoBackend,
  PostulanteInfo,
  DocPrepostulacion
} from '../../../services/documento.service';

export interface DocumentoUI {
  idTipoDocumento:  number;
  nombre:           string;
  descripcion:      string;
  obligatorio:      boolean;
  idDocumento:      number | null;
  archivo:          File | null;
  nombreArchivo:    string;
  estado:           'pendiente' | 'subido' | 'validado' | 'rechazado';
  observacion:      string;
  progreso:         number;
}

@Component({
  selector: 'app-subir-documentos',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './subir-documentos.html',
  styleUrls: ['./subir-documentos.scss']
})
export class SubirDocumentosComponent implements OnInit, OnDestroy {

  cargandoPagina       = true;
  mostrarModalExito    = false;
  documentoSubiendoId: number | null = null;

  postulante:    PostulanteInfo | null = null;
  idPostulacion: number | null = null;
  documentos:    DocumentoUI[] = [];
  docsPrepostulacion: DocPrepostulacion[] = [];

  // Selector de convocatoria
  misPostulaciones:       PostulanteInfo[] = [];
  idPostulacionSeleccion: number | null    = null;

  // SIEMPRE visible, bloqueado si solo hay 1
  get mostrarSelector(): boolean { return this.misPostulaciones.length >= 1; }
  get selectorBloqueado(): boolean { return this.misPostulaciones.length <= 1; }

  constructor(
    private router: Router,
    private documentoSvc: DocumentoService,
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const idUsuario = this.obtenerIdUsuarioActual();
    if (!idUsuario) { this.router.navigate(['/login']); return; }
    this.cargarPostulaciones(idUsuario);
  }

  ngOnDestroy(): void {}

  private cargarPostulaciones(idUsuario: number): void {
    this.documentoSvc.listarMisPostulaciones(idUsuario).subscribe({
      next: lista => {
        this.misPostulaciones = lista;
        this.idPostulacionSeleccion = lista.length > 0 ? lista[0].idPostulacion : null;
        this.cargarInfoPostulante(idUsuario);
        this.cdr.detectChanges();
      },
      error: () => this.fallbackCargarInfo(idUsuario)
    });
  }

  onConvocatoriaChange(idPostulacion: number): void {
    if (this.selectorBloqueado) return;
    this.idPostulacionSeleccion = idPostulacion;
    this.cargandoPagina         = true;
    this.documentos             = [];
    const idUsuario             = this.obtenerIdUsuarioActual();
    if (!idUsuario) return;

    this.documentoSvc.obtenerInfoPorConvocatoria(idUsuario, idPostulacion).subscribe({
      next: info => {
        this.postulante    = info;
        this.idPostulacion = info.idPostulacion;
        this.cargarDocumentos(info.idPostulacion);
        this.cdr.detectChanges();
      },
      error: () => {
        this.mostrarToast('error', 'Error', 'No se pudo cargar la convocatoria seleccionada.');
        this.cargandoPagina = false;
        this.cdr.detectChanges();
      }
    });
  }

  private cargarInfoPostulante(idUsuario: number): void {
    if (this.idPostulacionSeleccion) {
      this.documentoSvc.obtenerInfoPorConvocatoria(idUsuario, this.idPostulacionSeleccion).subscribe({
        next: info => {
          this.postulante    = info;
          this.idPostulacion = info.idPostulacion;
          this.cargarDocumentos(info.idPostulacion);
          this.cdr.detectChanges();
        },
        error: () => this.fallbackCargarInfo(idUsuario)
      });
    } else {
      this.fallbackCargarInfo(idUsuario);
    }
  }

  private fallbackCargarInfo(idUsuario: number): void {
    this.documentoSvc.obtenerInfoPostulante(idUsuario).subscribe({
      next: info => {
        this.postulante    = info;
        this.idPostulacion = info.idPostulacion;
        this.cdr.detectChanges();
        this.cargarDocumentos(info.idPostulacion);
      },
      error: () => {
        this.mostrarToast('error', 'Error', 'No se pudo cargar la información del postulante.');
        this.cargandoPagina = false;
        this.cdr.detectChanges();
      }
    });
  }

  private cargarDocumentos(idPostulacion: number): void {
    this.documentoSvc.obtenerDocumentosConvocatoria(idPostulacion).subscribe({
      next: docs => {
        this.documentos     = docs.map(d => this.mapearDocumento(d));
        this.cargandoPagina = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.mostrarToast('error', 'Error', 'No se pudo cargar la lista de documentos.');
        this.cargandoPagina = false;
        this.cdr.detectChanges();
      }
    });

    this.documentoSvc.obtenerDocsPrepostulacion(idPostulacion).subscribe({
      next: docs => { this.docsPrepostulacion = docs; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  nombreDesdeUrl(url: string): string {
    return url ? url.replace(/\\/g, '/').split('/').pop() ?? 'documento.pdf' : 'documento.pdf';
  }

  private mapearDocumento(d: DocumentoBackend): DocumentoUI {
    const estadoRaw = (d.estadoValidacion ?? '').toLowerCase().trim();
    let estado: DocumentoUI['estado'] = 'pendiente';
    if (d.idDocumento) {
      if      (estadoRaw === 'validado')  estado = 'validado';
      else if (estadoRaw === 'rechazado') estado = 'rechazado';
      else                                estado = 'subido';
    }
    const nombreArchivo = d.rutaArchivo
      ? d.rutaArchivo.replace(/\\/g, '/').split('/').pop() ?? '' : '';
    return {
      idTipoDocumento: d.idTipoDocumento, nombre: d.nombreTipo,
      descripcion:     d.descripcionTipo ?? 'Documento requerido para el proceso.',
      obligatorio:     d.obligatorio, idDocumento: d.idDocumento,
      archivo: null, nombreArchivo, estado,
      observacion: d.observacionesIa ?? '', progreso: d.idDocumento ? 100 : 0
    };
  }

  get totalSubidos(): number { return this.documentos.filter(d => d.estado === 'subido' || d.estado === 'validado').length; }
  get obligatoriosCompletos(): boolean { return this.documentos.filter(d => d.obligatorio).every(d => d.estado === 'subido' || d.estado === 'validado'); }
  get tieneDocRechazado(): boolean { return this.documentos.some(d => d.estado === 'rechazado'); }
  get porcentajeCompletado(): number { if (!this.documentos.length) return 0; return Math.round((this.totalSubidos / this.documentos.length) * 100); }
  get nombreCompleto(): string { if (!this.postulante) return ''; return `${this.postulante.nombres} ${this.postulante.apellidos}`; }
  get procesoLabel(): string {
    if (!this.postulante) return '';
    const conv = this.postulante.nombreConvocatoria ? `${this.postulante.nombreConvocatoria} — ` : '';
    return `${conv}${this.postulante.nombreMateria} · ${this.postulante.nombreCarrera}`;
  }

  onFileSelected(event: Event, doc: DocumentoUI): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const archivo = input.files[0];
    if (archivo.type !== 'application/pdf') { this.mostrarToast('error', 'Formato inválido', 'Solo se aceptan archivos PDF.'); input.value = ''; return; }
    if (archivo.size > 10 * 1024 * 1024) { this.mostrarToast('error', 'Archivo muy grande', 'El tamaño máximo es 10 MB.'); input.value = ''; return; }
    if (!this.idPostulacion) { this.mostrarToast('error', 'Error', 'No se encontró la postulación activa.'); return; }
    if (doc.idDocumento) {
      this.documentoSvc.eliminarDocumento(doc.idDocumento, this.idPostulacion).subscribe({ next: () => this.ejecutarSubida(doc, archivo, input), error: () => this.ejecutarSubida(doc, archivo, input) });
    } else { this.ejecutarSubida(doc, archivo, input); }
  }

  private ejecutarSubida(doc: DocumentoUI, archivo: File, input: HTMLInputElement): void {
    doc.archivo = archivo; doc.nombreArchivo = archivo.name; doc.progreso = 0; doc.estado = 'pendiente'; doc.idDocumento = null;
    this.documentoSubiendoId = doc.idTipoDocumento; this.cdr.detectChanges();
    const progreso$ = new Subject<number>();
    progreso$.subscribe(pct => { doc.progreso = pct; this.cdr.detectChanges(); });
    this.documentoSvc.subirDocumento(this.idPostulacion!, doc.idTipoDocumento, archivo, progreso$).subscribe({
      next: res => {
        if (res.exitoso) { doc.estado = 'subido'; doc.idDocumento = res.idDocumento; doc.progreso = 100; this.mostrarToast('success', '¡Cargado!', `"${doc.nombre}" subido correctamente.`); }
        else { this.limpiarDocUI(doc); this.mostrarToast('error', 'Error al subir', res.mensaje); }
        this.documentoSubiendoId = null; progreso$.complete(); input.value = ''; this.cdr.detectChanges();
      },
      error: () => { this.limpiarDocUI(doc); this.documentoSubiendoId = null; progreso$.complete(); input.value = ''; this.mostrarToast('error', 'Error', 'No se pudo conectar con el servidor.'); this.cdr.detectChanges(); }
    });
  }

  eliminarArchivo(doc: DocumentoUI): void {
    if (!doc.idDocumento || !this.idPostulacion) { this.limpiarDocUI(doc); return; }
    this.documentoSvc.eliminarDocumento(doc.idDocumento, this.idPostulacion).subscribe({
      next: res => { if (res.exitoso) { this.limpiarDocUI(doc); this.mostrarToast('info', 'Eliminado', 'El documento fue eliminado correctamente.'); } else { this.mostrarToast('error', 'Error', res.mensaje ?? 'No se pudo eliminar el documento.'); } this.cdr.detectChanges(); },
      error: () => { this.mostrarToast('error', 'Error', 'No se pudo conectar con el servidor.'); this.cdr.detectChanges(); }
    });
  }

  private limpiarDocUI(doc: DocumentoUI): void { doc.archivo = null; doc.nombreArchivo = ''; doc.estado = 'pendiente'; doc.progreso = 0; doc.idDocumento = null; this.cdr.detectChanges(); }

  triggerInput(idTipoDocumento: number): void {
    const input = document.getElementById(`file-input-${idTipoDocumento}`) as HTMLInputElement;
    if (input) input.click();
  }

  guardarYFinalizar(): void {
    if (!this.obligatoriosCompletos) { this.mostrarToast('error', 'Incompleto', 'Sube todos los documentos obligatorios (*) antes de finalizar.'); return; }
    if (!this.idPostulacion)         { this.mostrarToast('error', 'Error', 'No se encontró la postulación activa.'); return; }
    this.documentoSvc.finalizarCarga(this.idPostulacion).subscribe({
      next: res => {
        if (res.exitoso) {
          this.documentoSvc.notificarRevision(this.idPostulacion!).subscribe({ next: () => {}, error: () => {} });
          if (this.postulante) this.postulante = { ...this.postulante, estadoPostulacion: 'en_revision' };
          this.mostrarModalExito = true; this.cdr.detectChanges();
        } else { this.mostrarToast('error', 'No se pudo finalizar', res.mensaje); }
      },
      error: () => this.mostrarToast('error', 'Error', 'No se pudo conectar con el servidor.')
    });
  }

  volver(): void { this.router.navigate(['/postulante']); }
  mostrarToast(tipo: 'success' | 'error' | 'info', titulo: string, mensaje: string): void { this.toast[tipo](titulo, mensaje); }

  private obtenerIdUsuarioActual(): number | null {
    const token = localStorage.getItem('token');
    const idStr = localStorage.getItem('idUsuario');
    if (!token || !idStr) return null;
    const id = Number(idStr);
    return isNaN(id) || id <= 0 ? null : id;
  }
}
