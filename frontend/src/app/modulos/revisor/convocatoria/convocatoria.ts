import { Router } from '@angular/router';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ConvocatoriaAdminService,
  ConvocatoriaListaResponse,
  ConvocatoriaDetalleResponse,
  SolicitudResumen
} from '../../../services/convocatoria-admin.service';
import { SolicitudDocenteService, SolicitudDocenteResponse } from '../../../services/solicitud-docente.service';
import { TipoDocumentoService, TipoDocumento } from '../../../services/tipo-documento.service';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { AiConvocatoriaService } from '../../../services/ai-convocatoria.service';


// ── Reporte ──────────────────────────────────────────────────────────────────
interface ReporteConvocatoriaConfig {
  titulo: string;
  subtitulo: string;
  formato: 'PDF' | 'EXCEL';
  orientacion: 'VERTICAL' | 'HORIZONTAL';
  desde: string;
  hasta: string;
  estado: string;
  incluirPortada: boolean;
  incluirKpis: boolean;
  incluirDetalle: boolean;
  incluirGraficoEstados: boolean;
  incluirGraficoPrepostulaciones: boolean;
  incluirGraficoTemporal: boolean;
  colorPrimario: string;
  mostrarNumeroPagina: boolean;
  mostrarFechaGeneracion: boolean;
  excelCongelarEncabezado: boolean;
  excelFiltrosAutomaticos: boolean;
}

@Component({
  selector: 'app-convocatoria',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,

    ToastComponent
  ],
  templateUrl: './convocatoria.html',
  styleUrls: ['./convocatoria.scss']
})
export class ConvocatoriaComponent implements OnInit {

  // ===== Datos =====
  cargando = false;
  convocatorias: ConvocatoriaListaResponse[] = [];
  convocatoriasFiltradas: ConvocatoriaListaResponse[] = [];

  // ── PASO 6: Renombrado de solicitudesAprobadas → solicitudesDisponibles ──
  // Ahora solo muestra las que NO están ya en convocatorias activas
  solicitudesDisponibles: SolicitudDocenteResponse[] = [];
  tiposDocumentoDisponibles: TipoDocumento[] = [];

  // ===== Filtros =====
  search = '';
  filtroEstado = '';

  // ── Modal Reporte ─────────────────────────────────────────────────────────
  showReporteModal    = false;
  reporteGenerando    = false;
  reporteTabActiva: 'filtros' | 'secciones' | 'visual' | 'exportar' = 'filtros';
  reporteConfig: ReporteConvocatoriaConfig = {
    titulo: 'Reporte de Convocatorias',
    subtitulo: '',
    formato: 'PDF',
    orientacion: 'VERTICAL',
    desde: '',
    hasta: '',
    estado: '',
    incluirPortada: true,
    incluirKpis: true,
    incluirDetalle: true,
    incluirGraficoEstados: true,
    incluirGraficoPrepostulaciones: true,
    incluirGraficoTemporal: true,
    colorPrimario: '#00A63E',
    mostrarNumeroPagina: true,
    mostrarFechaGeneracion: true,
    excelCongelarEncabezado: true,
    excelFiltrosAutomaticos: true,
  };
  private readonly reporteApiUrl = 'http://localhost:8080/api/admin/convocatorias/reporte/generar';


  // ===== Paginación =====
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  get convocatoriasPaginadas(): ConvocatoriaListaResponse[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.convocatoriasFiltradas.slice(start, start + this.pageSize);
  }

  // ===== Estadísticas =====
  get totalAbiertas(): number {
    return this.convocatorias.filter(c => c.estadoConvocatoria === 'abierta').length;
  }
  get totalCerradas(): number {
    return this.convocatorias.filter(c => c.estadoConvocatoria === 'cerrada').length;
  }
  get totalCanceladas(): number {
    return this.convocatorias.filter(c => c.estadoConvocatoria === 'cancelada').length;
  }

  // ===== Modales =====
  modalAbierto = false;
  modalEstadoAbierto = false;
  modalDetalleAbierto = false;
  editando = false;
  isSaving = false;
  submitted = false;

  // ===== Formulario crear/editar =====
  form = {
    idConvocatoria:        0,
    titulo:                '',
    descripcion:           '',
    fechaPublicacion:      '',
    fechaInicio:           '',
    fechaFin:              '',
    fechaLimiteDocumentos: null as string | null,
    idsSolicitudes:        [] as number[],
    idsTiposDocumento:     [] as number[]
  };

  // ===== Modal cambiar estado =====
  convocatoriaSeleccionadaId = 0;
  nuevoEstado = 'abierta';

  // ===== Modal detalle =====
  detalleConvocatoria: ConvocatoriaDetalleResponse | null = null;

  // ===== IA =====
  generandoImagen = false;
  private toastImagenId: number | null = null;
  isGenerandoDescripcion = false;

  constructor(
    private http: HttpClient,
    private convocatoriaService: ConvocatoriaAdminService,
    private solicitudService: SolicitudDocenteService,
    private tipoDocumentoService: TipoDocumentoService,
    private cdr: ChangeDetectorRef,
    private toast: ToastService,
    private aiService: AiConvocatoriaService
    ,
    private router: Router) {}

  ngOnInit(): void {
    this.cargarConvocatorias();
    this.cargarSolicitudesDisponibles();  // ← PASO 6
    this.cargarTiposDocumento();
  }

  // =========================
  // LOADERS
  // =========================
  cargarConvocatorias(): void {
    this.cargando = true;
    this.convocatoriaService.listar().subscribe({
      next: (data) => {
        this.cargando = false;
        this.convocatorias = Array.isArray(data) ? data : [];
        this.convocatoriasFiltradas = [...this.convocatorias];
        this.calculatePagination();
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.convocatorias = [];
        this.convocatoriasFiltradas = [];
        this.calculatePagination();
        this.cdr.detectChanges();
      }
    });
  }

  // ── PASO 6: Usar el nuevo endpoint que filtra las ya asignadas ─────────
  cargarSolicitudesDisponibles(): void {
    this.convocatoriaService.getSolicitudesDisponibles().subscribe({
      next: (data) => {
        this.solicitudesDisponibles = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: () => { this.solicitudesDisponibles = []; }
    });
  }

  cargarTiposDocumento(): void {
    this.tipoDocumentoService.listar().subscribe({
      next: (data) => {
        this.tiposDocumentoDisponibles = data.filter(t => t.activo);
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  // =========================
  // FILTROS
  // =========================
  applyFilters(): void {
    const term = (this.search || '').trim().toLowerCase();
    this.convocatoriasFiltradas = this.convocatorias.filter(c => {
      const searchMatch = !term || c.titulo.toLowerCase().includes(term) || String(c.idConvocatoria).includes(term);
      const estadoMatch = !this.filtroEstado || c.estadoConvocatoria === this.filtroEstado;
      return searchMatch && estadoMatch;
    });
    this.currentPage = 1;
    this.calculatePagination();
  }

  // =========================
  // PAGINACIÓN
  // =========================
  calculatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.convocatoriasFiltradas.length / this.pageSize));
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    if (this.totalPages <= 5) {
      for (let i = 1; i <= this.totalPages; i++) pages.push(i);
      return pages;
    }
    pages.push(1);
    if (this.currentPage > 3) pages.push(-1);
    const start = Math.max(2, this.currentPage - 1);
    const end = Math.min(this.totalPages - 1, this.currentPage + 1);
    for (let i = start; i <= end; i++) pages.push(i);
    if (this.currentPage < this.totalPages - 2) pages.push(-1);
    pages.push(this.totalPages);
    return pages;
  }

  // =========================
  // UI HELPERS
  // =========================
  getEstadoClass(estado: string): string {
    switch (estado) {
      case 'abierta':   return 'success';
      case 'cerrada':   return 'warning';
      case 'cancelada': return 'danger';
      default:          return '';
    }
  }

  getEstadoLabel(estado: string): string {
    switch (estado) {
      case 'abierta':   return 'Abierta';
      case 'cerrada':   return 'Cerrada';
      case 'cancelada': return 'Cancelada';
      default:          return estado;
    }
  }

  isSolicitudSeleccionada(id: number): boolean {
    return this.form.idsSolicitudes.includes(id);
  }

  toggleSolicitud(id: number): void {
    const idx = this.form.idsSolicitudes.indexOf(id);
    if (idx === -1) this.form.idsSolicitudes.push(id);
    else this.form.idsSolicitudes.splice(idx, 1);
  }

  isTipoSeleccionado(id: number): boolean {
    return this.form.idsTiposDocumento.includes(id);
  }

  toggleTipoDocumento(id: number): void {
    const idx = this.form.idsTiposDocumento.indexOf(id);
    if (idx === -1) this.form.idsTiposDocumento.push(id);
    else this.form.idsTiposDocumento.splice(idx, 1);
  }

  // =========================
  // MODAL CREAR
  // =========================
  openCreate(): void {
    this.editando = false;
    this.submitted = false;
    const hoy = new Date().toISOString().split('T')[0];
    this.form = {
      idConvocatoria:        0,
      titulo:                '',
      descripcion:           '',
      fechaPublicacion:      hoy,
      fechaInicio:           '',
      fechaFin:              '',
      fechaLimiteDocumentos: null,
      idsSolicitudes:        [],
      idsTiposDocumento:     []
    };
    // Refrescar la lista de solicitudes disponibles al abrir el modal ──
    this.cargarSolicitudesDisponibles();
    this.modalAbierto = true;
  }

  // =========================
  // MODAL EDITAR
  // =========================
  edit(conv: ConvocatoriaListaResponse): void {
    this.editando = true;
    this.submitted = false;
    this.form = {
      idConvocatoria:        conv.idConvocatoria,
      titulo:                conv.titulo,
      descripcion:           conv.descripcion,
      fechaPublicacion:      conv.fechaPublicacion,
      fechaInicio:           conv.fechaInicio,
      fechaFin:              conv.fechaFin,
      fechaLimiteDocumentos: conv.fechaLimiteDocumentos ?? null,
      idsSolicitudes:        [],
      idsTiposDocumento:     []
    };
    this.modalAbierto = true;
  }

  // =========================
  // GUARDAR
  // =========================
  guardar(): void {
    this.submitted = true;

    if (!this.form.titulo.trim()) {
      this.toast.warning('Campo requerido', 'El título de la convocatoria es obligatorio.');
      return;
    }
    if (!this.form.fechaInicio || !this.form.fechaFin) {
      this.toast.warning('Campos requeridos', 'Las fechas de inicio y fin son obligatorias.');
      return;
    }
    if (this.form.fechaLimiteDocumentos && this.form.fechaFin &&
      this.form.fechaLimiteDocumentos > this.form.fechaFin) {
      this.toast.warning('Fecha inválida', 'La fecha límite de documentos no puede ser posterior a la fecha de fin.');
      return;
    }

    this.isSaving = true;

    if (this.editando) {
      this.convocatoriaService.actualizar(this.form.idConvocatoria, {
        titulo:                this.form.titulo,
        descripcion:           this.form.descripcion,
        fechaPublicacion:      this.form.fechaPublicacion,
        fechaInicio:           this.form.fechaInicio,
        fechaFin:              this.form.fechaFin,
        fechaLimiteDocumentos: this.form.fechaLimiteDocumentos || null,
        idsTiposDocumento:     this.form.idsTiposDocumento
      }).subscribe({
        next: () => {
          this.isSaving = false;
          this.closeModal();
          this.toast.success('Convocatoria actualizada', 'Los cambios se guardaron correctamente.');
          this.cargarConvocatorias();
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.cargando = false;
          this.isSaving = false;
          const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
          this.toast.error('No se pudo actualizar', msg);
        }
      });
    } else {
      this.convocatoriaService.crear({
        titulo:                this.form.titulo,
        descripcion:           this.form.descripcion,
        fechaPublicacion:      this.form.fechaPublicacion,
        fechaInicio:           this.form.fechaInicio,
        fechaFin:              this.form.fechaFin,
        fechaLimiteDocumentos: this.form.fechaLimiteDocumentos || null,
        idsSolicitudes:        this.form.idsSolicitudes,
        idsTiposDocumento:     this.form.idsTiposDocumento
      }).subscribe({
        next: (resp) => {
          this.isSaving = false;
          if (resp.exito && resp.data) {
            const idNueva = typeof resp.data === 'object'
              ? resp.data['idConvocatoria'] as number
              : resp.data as number;
            this.closeModal();
            this.generandoImagen = true;
            this.toastImagenId = this.toast.loading('Generando imagen de portada...', 'Esto puede tomar hasta 60 segundos');
            this.cdr.detectChanges();
            this.convocatoriaService.generarImagen(idNueva).subscribe({
              next: (r) => {
                if (this.toastImagenId) this.toast.remove(this.toastImagenId);
                this.generandoImagen = false;
                if (r.exito) {
                  this.toast.success('¡Convocatoria creada!', 'La imagen de portada se generó correctamente.');
                } else {
                  this.toast.warning('Convocatoria creada', 'No se pudo generar la imagen: ' + r.mensaje);
                }
                this.cargarConvocatorias();
                // ── PASO 6: Refrescar disponibles tras crear la convocatoria ──
                this.cargarSolicitudesDisponibles();
                this.cdr.detectChanges();
              },
              error: () => {
                this.cargando = false;
                if (this.toastImagenId) this.toast.remove(this.toastImagenId);
                this.generandoImagen = false;
                this.toast.warning('Convocatoria creada sin imagen', 'Puedes regenerarla desde el detalle.');
                this.cargarConvocatorias();
                this.cargarSolicitudesDisponibles();
                this.cdr.detectChanges();
              }
            });
          }
        },
        error: (err) => {
          this.cargando = false;
          this.isSaving = false;
          const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
          this.toast.error('No se pudo crear', msg);
        }
      });
    }
  }

  // =========================
  // MODAL CAMBIAR ESTADO
  // =========================
  openCambiarEstado(conv: ConvocatoriaListaResponse): void {
    this.convocatoriaSeleccionadaId = conv.idConvocatoria;
    this.nuevoEstado = conv.estadoConvocatoria;
    this.modalEstadoAbierto = true;
  }

  guardarEstado(): void {
    this.isSaving = true;
    this.convocatoriaService.cambiarEstado(this.convocatoriaSeleccionadaId, this.nuevoEstado).subscribe({
      next: () => {
        this.isSaving = false;
        this.modalEstadoAbierto = false;
        this.toast.success('Estado actualizado', `La convocatoria ahora está ${this.nuevoEstado}.`);
        this.cargarConvocatorias();
        // ── PASO 6: Al cerrar convocatoria las solicitudes quedan disponibles de nuevo ──
        this.cargarSolicitudesDisponibles();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.isSaving = false;
        const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
        this.toast.error('No se pudo cambiar el estado', msg);
      }
    });
  }

  // =========================
  // MODAL DETALLE
  // =========================
  verDetalle(id: number): void {
    this.detalleConvocatoria = null;
    this.modalDetalleAbierto = true;
    this.convocatoriaService.detalle(id).subscribe({
      next: (data) => {
        this.cargando = false;
        this.detalleConvocatoria = data;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.modalDetalleAbierto = false;
        this.toast.error('Error', 'No se pudo cargar el detalle de la convocatoria.');
      }
    });
  }

  // =========================
  // ELIMINAR
  // =========================
  eliminar(conv: ConvocatoriaListaResponse): void {
    if (!confirm(`¿Seguro que deseas eliminar la convocatoria "${conv.titulo}"? Esta acción no se puede deshacer.`)) return;
    this.convocatoriaService.eliminar(conv.idConvocatoria).subscribe({
      next: () => {
        this.toast.success('Convocatoria eliminada', `"${conv.titulo}" fue eliminada correctamente.`);
        this.cargarConvocatorias();
        this.cargarSolicitudesDisponibles();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
        this.toast.error('No se pudo eliminar', msg);
      }
    });
  }

  // =========================
  // CERRAR MODALES
  // =========================
  closeModal(): void {
    this.modalAbierto = false;
    this.submitted = false;
    this.cdr.detectChanges();
  }

  closeModalEstado(): void {
    this.modalEstadoAbierto = false;
    this.cdr.detectChanges();
  }

  closeModalDetalle(): void {
    this.modalDetalleAbierto = false;
    this.detalleConvocatoria = null;
    this.cdr.detectChanges();
  }

  regenerarImagen(id: number): void {
    this.generandoImagen = true;
    this.toastImagenId = this.toast.loading('Regenerando imagen...', 'Puede tomar hasta 60 segundos');
    this.cdr.detectChanges();
    this.convocatoriaService.generarImagen(id).subscribe({
      next: (resp) => {
        if (this.toastImagenId) this.toast.remove(this.toastImagenId);
        this.generandoImagen = false;
        if (resp.exito && resp.data) {
          if (this.detalleConvocatoria) this.detalleConvocatoria.imagenPortadaUrl = resp.data as string;
          this.toast.success('¡Imagen regenerada!', 'La nueva portada se asignó a la convocatoria.');
        } else {
          this.toast.warning('No se pudo regenerar', resp.mensaje);
        }
        this.cargarConvocatorias();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        if (this.toastImagenId) this.toast.remove(this.toastImagenId);
        this.generandoImagen = false;
        const msg = err?.error?.mensaje || 'El modelo de IA no está disponible ahora.';
        this.toast.error('Error al regenerar imagen', msg);
        this.cdr.detectChanges();
      }
    });
  }

  // ── FEATURE 4: Generar descripción con IA ─────────────────────────────
  generarDescripcionConIA(): void {
    if (this.form.idsSolicitudes.length === 0) {
      this.toast.warning('Sin solicitudes', 'Selecciona al menos una solicitud para generar la descripción.');
      return;
    }

    this.isGenerandoDescripcion = true;
    const toastId = this.toast.loading('Generando descripción con IA...', 'Esto puede tardar unos segundos');
    this.cdr.detectChanges();

    this.aiService.generarDescripcion({ idsSolicitudes: this.form.idsSolicitudes }).subscribe({
      next: (resp) => {
        this.toast.remove(toastId);
        this.isGenerandoDescripcion = false;
        if (resp.descripcion) {
          this.form.descripcion = resp.descripcion;
          this.toast.success('Descripción generada', 'Puedes editarla antes de guardar.');
        } else {
          this.toast.error('Error', resp.error || 'No se pudo generar la descripción.');
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.toast.remove(toastId);
        this.isGenerandoDescripcion = false;
        this.toast.error('IA no disponible', 'Verifica que el servicio de IA esté activo.');
        this.cdr.detectChanges();
      }
    });
  }
  // ── Métodos Reporte ───────────────────────────────────────────────────────
  exportarReporte(): void {
    this.router.navigate(['/revisor/estadisticas-convocatorias']);
  }

  cerrarReporteModal(): void { this.showReporteModal = false; }

  cambiarReporteTab(tab: 'filtros' | 'secciones' | 'visual' | 'exportar'): void {
    this.reporteTabActiva = tab;
  }

  get reporteSeccionesActivas(): number {
    const s = this.reporteConfig;
    return [s.incluirPortada, s.incluirKpis, s.incluirDetalle,
      s.incluirGraficoEstados, s.incluirGraficoPrepostulaciones, s.incluirGraficoTemporal]
      .filter(Boolean).length;
  }

  generarReporte(): void {
    this.reporteGenerando = true;
    this.http.post(this.reporteApiUrl, this.reporteConfig, {
      responseType: 'blob', observe: 'response'
    }).subscribe({
      next: (response) => {
        const blob = response.body!;
        const cd   = response.headers.get('content-disposition') || '';
        const match = cd.match(/filename="?([^";\n]+)"?/);
        const nombre = match ? match[1]
          : `reporte_convocatorias.${this.reporteConfig.formato === 'EXCEL' ? 'xlsx' : 'pdf'}`;
        const url = window.URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href = url; a.download = nombre; a.click();
        window.URL.revokeObjectURL(url);
        this.reporteGenerando = false;
        this.cerrarReporteModal();
        this.toast.success('Reporte generado correctamente');
        this.cdr.detectChanges();
      },
      error: () => {
        this.reporteGenerando = false;
        this.toast.error('Error al generar el reporte');
        this.cdr.detectChanges();
      }
    });
  }

}
