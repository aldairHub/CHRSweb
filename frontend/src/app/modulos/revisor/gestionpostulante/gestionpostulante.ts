import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import {
  PrepostulacionService,
  Prepostulacion,
  Convocatoria,
  SolicitudDocente,
  DetallePostulacion
} from '../../../services/prepostulacion.service';

export interface DocumentoAcademico {
  idDocumento: number;
  descripcion: string;
  urlDocumento: string;
  fechaSubida: string;
}

@Component({
  selector: 'app-gestion-postulante',
  standalone: true,
  templateUrl: './gestionpostulante.html',
  styleUrls: ['./gestionpostulante.scss'],
  imports: [CommonModule, FormsModule, DatePipe, NavbarComponent]
})
export class GestionPostulanteComponent implements OnInit {

  documentos: any[] = [];
  documentosFiltrados: any[] = [];
  documentosPaginados: any[] = [];
  selectedDocumento: any = null;
  showDocumentosModal = false;
  showRechazarModal = false;
  showDetalleModal = false;
  motivoRechazo = '';

  // Detalle convocatoria/solicitud
  detallePostulacion: DetallePostulacion | null = null;
  cargandoDetalle = false;

  totalDocumentos = 0;
  documentosPendientes = 0;
  documentosValidados = 0;
  documentosRechazados = 0;

  searchTerm = '';
  filterEstado = '';
  filterConvocatoria = '';
  filterSolicitud = '';

  convocatorias: Convocatoria[] = [];
  solicitudes: SolicitudDocente[] = [];

  // Modal filtro avanzado
  showFiltroModal = false;
  filtroModalConvocatoria = '';
  filtroModalSolicitud = '';
  String = String;

  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;
  cargando = true;

  showToast = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  constructor(
    private prepostulacionService: PrepostulacionService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarDesdeBackend();
    this.cargarFiltros();
  }

  // ── Carga inicial ─────────────────────────────────────────

  cargarDesdeBackend(): void {
    this.cargando = true;
    this.prepostulacionService.listarPrepostulaciones().subscribe({
      next: (data: Prepostulacion[]) => {
        this.documentos = data.map(p => ({
          id: p.idPrepostulacion,
          nombreCompleto: `${p.nombres} ${p.apellidos}`,
          cedula: p.identificacion,
          correo: p.correo,
          estado: this.mapEstado(p.estadoRevision),
          fechaEnvio: new Date(p.fechaEnvio),
          urlCedula: p.urlCedula || '',
          urlFoto: p.urlFoto || '',
          documentosAcademicos: [],
          idConvocatoria: (p as any).idConvocatoria || null,
          idSolicitud: (p as any).idSolicitud || null,
          tituloConvocatoria: (p as any).tituloConvocatoria || '',
          nombreSolicitud: (p as any).nombreSolicitud || ''
        }));
        this.applyFilters();
        this.calcularEstadisticas();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarFiltros(): void {
    this.prepostulacionService.listarConvocatorias().subscribe({
      next: (data) => { this.convocatorias = data; this.cdr.detectChanges(); },
      error: () => {}
    });
    this.prepostulacionService.listarSolicitudes().subscribe({
      next: (data) => { this.solicitudes = data; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  mapEstado(estado: string): string {
    if (estado === 'APROBADO') return 'validado';
    if (estado === 'RECHAZADO') return 'rechazado';
    return 'pendiente';
  }

  // ── Modal documentos ──────────────────────────────────────

  verDocumentos(doc: any): void {
    this.selectedDocumento = { ...doc };
    this.showDocumentosModal = true;
    this.prepostulacionService.obtenerDocumentos(doc.id).subscribe({
      next: (data: any) => {
        this.selectedDocumento.urlCedula = data.cedula || '';
        this.selectedDocumento.urlFoto   = data.foto   || '';
        this.selectedDocumento.documentosAcademicos = data.documentosAcademicos || [];
        this.cdr.detectChanges();
      },
      error: () => alert('Error al cargar los documentos')
    });
  }

  closeDocumentosModal(): void {
    this.showDocumentosModal = false;
    this.selectedDocumento = null;
    this.cdr.detectChanges();
  }

  // ── Modal detalle convocatoria/solicitud ──────────────────

  verDetalle(doc: any): void {
    this.selectedDocumento = { ...doc };
    this.detallePostulacion = null;
    this.cargandoDetalle = true;
    this.showDetalleModal = true;
    this.cdr.detectChanges();

    this.prepostulacionService.obtenerDetalle(doc.id).subscribe({
      next: (data: DetallePostulacion) => {
        this.detallePostulacion = data;
        this.cargandoDetalle = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoDetalle = false;
        this.mostrarToast('error', 'Error al cargar el detalle');
        this.cdr.detectChanges();
      }
    });
  }

  closeDetalleModal(): void {
    this.showDetalleModal = false;
    this.detallePostulacion = null;
    this.selectedDocumento = null;
    this.cdr.detectChanges();
  }

  // ── Documentos ────────────────────────────────────────────

  verDocumento(doc: any): void {
    if (!doc.url) { alert('No hay archivo disponible.'); return; }
    window.open(doc.url, '_blank');
  }

  descargarDocumento(doc: { url: string; nombre: string }): void {
    if (!doc.url) { alert('No hay archivo disponible.'); return; }
    fetch(doc.url)
      .then(res => { if (!res.ok) throw new Error(); return res.blob(); })
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = doc.nombre || 'documento';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(a.href);
      })
      .catch(() => window.open(doc.url, '_blank'));
  }

  descargarDocumentos(doc?: any): void {
    if (!doc) return;
    this.prepostulacionService.obtenerDocumentos(doc.id).subscribe({
      next: (data: any) => {
        const archivos: { url: string; nombre: string }[] = [];
        if (data.cedula) archivos.push({ url: data.cedula, nombre: 'cedula.pdf' });
        if (data.foto)   archivos.push({ url: data.foto,   nombre: 'foto.jpg' });
        if (data.documentosAcademicos?.length) {
          data.documentosAcademicos.forEach((d: DocumentoAcademico, i: number) => {
            archivos.push({ url: d.urlDocumento, nombre: `${d.descripcion || 'titulo_' + (i + 1)}.pdf` });
          });
        }
        archivos.forEach((archivo, index) => {
          setTimeout(() => this.descargarDocumento(archivo), index * 400);
        });
      },
      error: () => alert('Error al descargar los documentos')
    });
  }

  // ── Validar / Rechazar ────────────────────────────────────

  validarDocumentos(doc: any): void {
    const request = { estado: 'APROBADO', observaciones: 'Documentos validados correctamente', idRevisor: 1 };
    this.prepostulacionService.actualizarEstado(doc.id, request).subscribe({
      next: () => {
        const item = this.documentos.find(d => d.id === doc.id);
        if (item) item.estado = 'validado';
        if (this.selectedDocumento?.id === doc.id) {
          this.selectedDocumento = { ...this.selectedDocumento, estado: 'validado' };
        }
        this.calcularEstadisticas();
        this.applyFilters();
        this.cdr.detectChanges();
        this.mostrarToast('success', 'Documentos validados correctamente');
      },
      error: (err: any) => this.mostrarToast('error', `Error al validar. Código: ${err.status}`)
    });
  }

  validarTodosDocumentos(): void {
    if (!this.selectedDocumento) return;
    this.validarDocumentos(this.selectedDocumento);
    this.closeDocumentosModal();
  }

  rechazarDocumentos(doc: any): void {
    this.selectedDocumento = doc;
    this.motivoRechazo = '';
    this.showRechazarModal = true;
    this.cdr.detectChanges();
  }

  rechazarTodosDocumentos(): void {
    if (!this.selectedDocumento) return;
    this.motivoRechazo = '';
    this.showRechazarModal = true;
    this.cdr.detectChanges();
  }

  confirmarRechazo(): void {
    if (!this.selectedDocumento || !this.motivoRechazo.trim()) return;
    const request = { estado: 'RECHAZADO', observaciones: this.motivoRechazo, idRevisor: 1 };
    this.prepostulacionService.actualizarEstado(this.selectedDocumento.id, request).subscribe({
      next: () => {
        const item = this.documentos.find(d => d.id === this.selectedDocumento.id);
        if (item) item.estado = 'rechazado';
        this.closeRechazarModal();
        this.closeDocumentosModal();
        this.calcularEstadisticas();
        this.applyFilters();
        this.cdr.detectChanges();
        this.mostrarToast('success', 'Documentos rechazados correctamente');
      },
      error: (err: any) => this.mostrarToast('error', `Error al rechazar. Código: ${err.status}`)
    });
  }

  closeRechazarModal(): void {
    this.showRechazarModal = false;
    this.motivoRechazo = '';
    this.cdr.detectChanges();
  }

  // ── Filtros y paginación ──────────────────────────────────

  applyFilters(): void {
    this.documentosFiltrados = this.documentos.filter(d => {
      const matchSearch =
        d.nombreCompleto.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        d.cedula.includes(this.searchTerm) ||
        d.correo.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchEstado       = !this.filterEstado       || d.estado === this.filterEstado;
      const matchConvocatoria = !this.filterConvocatoria || String(d.idConvocatoria) === this.filterConvocatoria;
      const matchSolicitud    = !this.filterSolicitud    || String(d.idSolicitud)    === this.filterSolicitud;
      return matchSearch && matchEstado && matchConvocatoria && matchSolicitud;
    });
    this.currentPage = 1;
    this.updatePagination();
    this.cdr.detectChanges();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.documentosFiltrados.length / this.pageSize);
    const start = (this.currentPage - 1) * this.pageSize;
    this.documentosPaginados = this.documentosFiltrados.slice(start, start + this.pageSize);
    this.cdr.detectChanges();
  }

  changePage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePagination();
  }

  getPageNumbers(): number[] {
    if (this.totalPages <= 5) return Array.from({ length: this.totalPages }, (_, i) => i + 1);
    const pages: number[] = [1];
    if (this.currentPage > 3) pages.push(-1);
    for (let i = Math.max(2, this.currentPage - 1); i <= Math.min(this.totalPages - 1, this.currentPage + 1); i++) pages.push(i);
    if (this.currentPage < this.totalPages - 2) pages.push(-1);
    if (this.totalPages > 1) pages.push(this.totalPages);
    return pages;
  }

  calcularEstadisticas(): void {
    this.totalDocumentos    = this.documentos.length;
    this.documentosPendientes = this.documentos.filter(d => d.estado === 'pendiente').length;
    this.documentosValidados  = this.documentos.filter(d => d.estado === 'validado').length;
    this.documentosRechazados = this.documentos.filter(d => d.estado === 'rechazado').length;
    this.cdr.detectChanges();
  }

  // ── Helpers UI ────────────────────────────────────────────

  getEstadoBadgeClass(estado: string): string {
    return ({ pendiente: 'badge-warning', validado: 'badge-success', rechazado: 'badge-danger' } as any)[estado] || 'badge-secondary';
  }

  getEstadoLabel(estado: string): string {
    return estado.charAt(0).toUpperCase() + estado.slice(1);
  }

  getEstadoConvocatoriaClass(estado: string): string {
    const map: any = { ACTIVA: 'badge-success', CERRADA: 'badge-danger', PROXIMA: 'badge-warning' };
    return map[estado?.toUpperCase()] || 'badge-secondary';
  }

  getNivelAcademicoLabel(nivel: string): string {
    const map: any = {
      MAESTRIA: 'Maestría', DOCTORADO: 'Doctorado',
      LICENCIATURA: 'Licenciatura', INGENIERIA: 'Ingeniería'
    };
    return map[nivel?.toUpperCase()] || nivel || '—';
  }

  // ── Modal filtro avanzado ─────────────────────────────────

  get solicitudesFiltroModal(): SolicitudDocente[] {
    if (!this.filtroModalConvocatoria) return this.solicitudes;
    // Buscar qué solicitudes pertenecen a la convocatoria seleccionada
    // mirando los documentos cargados que ya tienen idConvocatoria e idSolicitud
    const idsSolicitud = new Set(
      this.documentos
        .filter(d => String(d.idConvocatoria) === this.filtroModalConvocatoria && d.idSolicitud)
        .map(d => d.idSolicitud)
    );
    return this.solicitudes.filter(s => idsSolicitud.has(s.idSolicitud));
  }

  openFiltroModal(): void {
    this.filtroModalConvocatoria = this.filterConvocatoria;
    this.filtroModalSolicitud    = this.filterSolicitud;
    this.showFiltroModal = true;
  }

  closeFiltroModal(): void {
    this.showFiltroModal = false;
  }

  seleccionarConvocatoriaModal(id: string): void {
    this.filtroModalConvocatoria = id;
    this.filtroModalSolicitud    = ''; // reset solicitud al cambiar convocatoria
  }

  limpiarFiltroModal(): void {
    this.filtroModalConvocatoria = '';
    this.filtroModalSolicitud    = '';
  }

  aplicarFiltroModal(): void {
    this.filterConvocatoria = this.filtroModalConvocatoria;
    this.filterSolicitud    = this.filtroModalSolicitud;
    this.applyFilters();
    this.closeFiltroModal();
  }

  getConvocatoriaNombre(id: string): string {
    const c = this.convocatorias.find(x => String(x.idConvocatoria) === id);
    return c ? c.titulo : `Convocatoria #${id}`;
  }

  clearFiltroConvocatoria(): void {
    this.filterConvocatoria = '';
    this.filterSolicitud    = '';
    this.applyFilters();
  }

  clearFiltroSolicitud(): void {
    this.filterSolicitud = '';
    this.applyFilters();
  }

  clearAllFiltros(): void {
    this.filterConvocatoria = '';
    this.filterSolicitud    = '';
    this.applyFilters();
  }

  exportarReporte(): void { alert('Funcionalidad de exportación pendiente'); }

  mostrarToast(tipo: 'success' | 'error', mensaje: string): void {
    this.toastType = tipo;
    this.toastMessage = mensaje;
    this.showToast = true;
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => {
      this.showToast = false;
      this.cdr.detectChanges();
    }, 2500);
  }
}
