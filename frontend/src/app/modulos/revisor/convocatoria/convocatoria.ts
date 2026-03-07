import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer.component';
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

@Component({
  selector: 'app-convocatoria',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent,
    ToastComponent
  ],
  templateUrl: './convocatoria.html',
  styleUrls: ['./convocatoria.scss']
})
export class ConvocatoriaComponent implements OnInit {

  // ===== Datos =====
  convocatorias: ConvocatoriaListaResponse[] = [];
  convocatoriasFiltradas: ConvocatoriaListaResponse[] = [];
  solicitudesAprobadas: SolicitudDocenteResponse[] = [];
  tiposDocumentoDisponibles: TipoDocumento[] = [];   // NUEVO

  // ===== Filtros =====
  search = '';
  filtroEstado = '';

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
    fechaLimiteDocumentos: null as string | null,  // NUEVO
    idsSolicitudes:        [] as number[],
    idsTiposDocumento:     [] as number[]          // NUEVO
  };

  // ===== Modal cambiar estado =====
  convocatoriaSeleccionadaId = 0;
  nuevoEstado = 'abierta';

  // ===== Modal detalle =====
  detalleConvocatoria: ConvocatoriaDetalleResponse | null = null;

  // ===== Generación de imagen IA =====
  generandoImagen = false;
  private toastImagenId: number | null = null;

  constructor(
    private convocatoriaService: ConvocatoriaAdminService,
    private solicitudService: SolicitudDocenteService,
    private tipoDocumentoService: TipoDocumentoService,   // NUEVO
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarConvocatorias();
    this.cargarSolicitudesAprobadas();
    this.cargarTiposDocumento();  // NUEVO
  }

  // =========================
  // LOADERS
  // =========================
  cargarConvocatorias(): void {
    this.convocatoriaService.listar().subscribe({
      next: (data) => {
        this.convocatorias = Array.isArray(data) ? data : [];
        this.convocatoriasFiltradas = [...this.convocatorias];
        this.calculatePagination();
        this.cdr.detectChanges();
      },
      error: () => {
        this.convocatorias = [];
        this.convocatoriasFiltradas = [];
        this.calculatePagination();
        this.cdr.detectChanges();
      }
    });
  }

  cargarSolicitudesAprobadas(): void {
    this.solicitudService.obtenerSolicitudesPorEstado('aprobada').subscribe({
      next: (data) => {
        this.solicitudesAprobadas = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: () => { this.solicitudesAprobadas = []; }
    });
  }

  // NUEVO
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

  // NUEVO
  isTipoSeleccionado(id: number): boolean {
    return this.form.idsTiposDocumento.includes(id);
  }

  // NUEVO
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
    // Validar que fecha límite docs no supere fecha fin
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
        next: (resp) => {
          this.isSaving = false;
          this.closeModal();
          this.toast.success('Convocatoria actualizada', 'Los cambios se guardaron correctamente.');
          this.cargarConvocatorias();
          this.cdr.detectChanges();
        },
        error: (err) => {
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
                this.cdr.detectChanges();
              },
              error: () => {
                if (this.toastImagenId) this.toast.remove(this.toastImagenId);
                this.generandoImagen = false;
                this.toast.warning('Convocatoria creada sin imagen', 'Puedes regenerarla desde el detalle.');
                this.cargarConvocatorias();
                this.cdr.detectChanges();
              }
            });
          }
        },
        error: (err) => {
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
        this.cdr.detectChanges();
      },
      error: (err) => {
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
        this.detalleConvocatoria = data;
        this.cdr.detectChanges();
      },
      error: () => {
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
        this.cdr.detectChanges();
      },
      error: (err) => {
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
        if (this.toastImagenId) this.toast.remove(this.toastImagenId);
        this.generandoImagen = false;
        const msg = err?.error?.mensaje || 'El modelo de IA no está disponible ahora.';
        this.toast.error('Error al regenerar imagen', msg);
        this.cdr.detectChanges();
      }
    });
  }
}
