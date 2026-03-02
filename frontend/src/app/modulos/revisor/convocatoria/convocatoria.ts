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
import { SolicitudDocenteService, SolicitudDocenteResponse } from '../../../services/SolicitudDocente.service';

@Component({
  selector: 'app-convocatoria',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent
  ],
  templateUrl: './convocatoria.html',
  styleUrls: ['./convocatoria.scss']
})
export class ConvocatoriaComponent implements OnInit {

  // ===== Datos =====
  convocatorias: ConvocatoriaListaResponse[] = [];
  convocatoriasFiltradas: ConvocatoriaListaResponse[] = [];
  solicitudesAprobadas: SolicitudDocenteResponse[] = [];

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
    idConvocatoria: 0,
    titulo: '',
    descripcion: '',
    fechaPublicacion: '',
    fechaInicio: '',
    fechaFin: '',
    idsSolicitudes: [] as number[]
  };

  // ===== Modal cambiar estado =====
  convocatoriaSeleccionadaId = 0;
  nuevoEstado = 'abierta';

  // ===== Modal detalle =====
  detalleConvocatoria: ConvocatoriaDetalleResponse | null = null;

  constructor(
    private convocatoriaService: ConvocatoriaAdminService,
    private solicitudService: SolicitudDocenteService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarConvocatorias();
    this.cargarSolicitudesAprobadas();
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
      error: (err) => {
        console.error('Error al cargar convocatorias:', err);
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
      error: (err) => {
        console.error('Error al cargar solicitudes aprobadas:', err);
        this.solicitudesAprobadas = [];
      }
    });
  }

  // =========================
  // FILTROS
  // =========================
  applyFilters(): void {
    const term = (this.search || '').trim().toLowerCase();

    this.convocatoriasFiltradas = this.convocatorias.filter(c => {
      const searchMatch =
        !term ||
        c.titulo.toLowerCase().includes(term) ||
        String(c.idConvocatoria).includes(term);

      const estadoMatch =
        !this.filtroEstado || c.estadoConvocatoria === this.filtroEstado;

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
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;

    if (this.totalPages <= maxVisible) {
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
    if (idx === -1) {
      this.form.idsSolicitudes.push(id);
    } else {
      this.form.idsSolicitudes.splice(idx, 1);
    }
  }

  // =========================
  // MODAL CREAR
  // =========================
  openCreate(): void {
    this.editando = false;
    this.submitted = false;
    const hoy = new Date().toISOString().split('T')[0];
    this.form = {
      idConvocatoria: 0,
      titulo: '',
      descripcion: '',
      fechaPublicacion: hoy,
      fechaInicio: '',
      fechaFin: '',
      idsSolicitudes: []
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
      idConvocatoria: conv.idConvocatoria,
      titulo: conv.titulo,
      descripcion: conv.descripcion,
      fechaPublicacion: conv.fechaPublicacion,
      fechaInicio: conv.fechaInicio,
      fechaFin: conv.fechaFin,
      idsSolicitudes: []
    };
    this.modalAbierto = true;
  }

  // =========================
  // GUARDAR
  // =========================
  guardar(): void {
    this.submitted = true;

    if (!this.form.titulo.trim()) {
      alert('El título es obligatorio.');
      return;
    }
    if (!this.form.fechaInicio || !this.form.fechaFin) {
      alert('Las fechas de inicio y fin son obligatorias.');
      return;
    }

    this.isSaving = true;

    if (this.editando) {
      this.convocatoriaService.actualizar(this.form.idConvocatoria, {
        titulo: this.form.titulo,
        descripcion: this.form.descripcion,
        fechaPublicacion: this.form.fechaPublicacion,
        fechaInicio: this.form.fechaInicio,
        fechaFin: this.form.fechaFin
      }).subscribe({
        next: (resp) => {
          this.isSaving = false;
          this.closeModal();
          alert('✅ Convocatoria actualizada con éxito.');
          this.cargarConvocatorias();
          this.cdr.detectChanges();

        },
        error: (err) => {
          this.isSaving = false;
          const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
          alert('❌ No se pudo actualizar: ' + msg);
        }
      });
    } else {
      this.convocatoriaService.crear({
        titulo: this.form.titulo,
        descripcion: this.form.descripcion,
        fechaPublicacion: this.form.fechaPublicacion,
        fechaInicio: this.form.fechaInicio,
        fechaFin: this.form.fechaFin,
        idsSolicitudes: this.form.idsSolicitudes
      }).subscribe({
        next: (resp) => {
          this.isSaving = false;
          this.closeModal();
          alert('✅ Convocatoria creada con éxito.');
          this.cargarConvocatorias();
          this.cdr.detectChanges();

        },
        error: (err) => {
          this.isSaving = false;
          const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
          alert('❌ No se pudo crear: ' + msg);
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
        alert('✅ Estado actualizado con éxito.');
        this.cargarConvocatorias();
        this.cdr.detectChanges();

      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
        alert('❌ No se pudo cambiar el estado: ' + msg);
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
      error: (err) => {
        console.error('Error al cargar detalle:', err);
        this.modalDetalleAbierto = false;
        alert('❌ No se pudo cargar el detalle.');
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
        alert('✅ Convocatoria eliminada.');
        this.cargarConvocatorias();
        this.cdr.detectChanges();

      },
      error: (err) => {
        const msg = err?.error?.mensaje || err?.error?.message || 'Error desconocido';
        alert('❌ No se pudo eliminar: ' + msg);
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
}
