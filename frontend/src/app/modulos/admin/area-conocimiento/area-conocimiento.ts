import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';
import { AreaConocimientoService, AreaConocimiento } from '../../../services/area-conocimiento.service';

@Component({
  selector: 'app-area-conocimiento',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './area-conocimiento.html',
  styleUrls: ['./area-conocimiento.scss']
})
export class AreaConocimientoComponent implements OnInit {

  // ===== Datos =====
  cargando = false;
  areas: AreaConocimiento[] = [];
  areasFiltradas: AreaConocimiento[] = [];

  // ===== Filtros =====
  search = '';

  // ===== Paginación =====
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  get areasPaginadas(): AreaConocimiento[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.areasFiltradas.slice(start, start + this.pageSize);
  }

  // ===== Modal =====
  modalAbierto = false;
  editando = false;
  isSaving = false;
  submitted = false;
  mostrarConfirmEliminar = false;
  areaAEliminar: AreaConocimiento | null = null;

  // ===== Formulario =====
  form = { id: 0, nombreArea: '' };

  constructor(
    private areaService: AreaConocimientoService,
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarAreas();
  }

  // =========================
  // LOADERS
  // =========================
  cargarAreas(): void {
    this.cargando = true;
    this.areaService.listar().subscribe({
      next: (data) => {
        this.areas = Array.isArray(data) ? data : [];
        this.areasFiltradas = [...this.areas];
        this.calculatePagination();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.areas = [];
        this.areasFiltradas = [];
        this.toast.error('Error', 'No se pudieron cargar las áreas de conocimiento.');
        this.cdr.detectChanges();
      }
    });
  }

  // =========================
  // FILTROS
  // =========================
  applyFilters(): void {
    const term = (this.search || '').trim().toLowerCase();
    this.areasFiltradas = this.areas.filter(a =>
      !term || a.nombreArea.toLowerCase().includes(term)
    );
    this.currentPage = 1;
    this.calculatePagination();
  }

  // =========================
  // PAGINACIÓN
  // =========================
  calculatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.areasFiltradas.length / this.pageSize));
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
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
  // MODAL CREAR
  // =========================
  openCreate(): void {
    this.editando = false;
    this.submitted = false;
    this.form = { id: 0, nombreArea: '' };
    this.modalAbierto = true;
  }

  // =========================
  // MODAL EDITAR
  // =========================
  edit(area: AreaConocimiento): void {
    this.editando = true;
    this.submitted = false;
    this.form = { id: area.idArea, nombreArea: area.nombreArea };
    this.modalAbierto = true;
  }

  // =========================
  // GUARDAR
  // =========================
  guardar(): void {
    this.submitted = true;
    if (!this.form.nombreArea?.trim()) {
      this.toast.warning('Campo requerido', 'El nombre del área es obligatorio.');
      return;
    }

    this.isSaving = true;
    const nombre = this.form.nombreArea.trim();
    const loadId = this.toast.loading(this.editando ? 'Actualizando área...' : 'Creando área...');

    const request = this.editando
      ? this.areaService.actualizar(this.form.id, nombre)
      : this.areaService.crear(nombre);

    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.toast.remove(loadId);
        this.toast.success(
          this.editando ? 'Área actualizada' : 'Área creada',
          `"${nombre}" fue ${this.editando ? 'actualizada' : 'creada'} correctamente.`
        );
        this.closeModal();
        this.cargarAreas();
      },
      error: (err) => {
        this.isSaving = false;
        this.toast.remove(loadId);
        const msg = err?.error?.message || err?.message || 'Intenta de nuevo.';
        this.toast.error('No se pudo guardar', msg);
      }
    });
  }

  // =========================
  // ELIMINAR
  // =========================
  confirmarEliminar(area: AreaConocimiento): void {
    this.areaAEliminar = area;
    this.mostrarConfirmEliminar = true;
  }

  eliminar(): void {
    if (!this.areaAEliminar) return;
    const nombre = this.areaAEliminar.nombreArea;
    const loadId = this.toast.loading('Eliminando área...');

    this.areaService.eliminar(this.areaAEliminar.idArea).subscribe({
      next: () => {
        this.toast.remove(loadId);
        this.toast.success('Área eliminada', `"${nombre}" fue eliminada correctamente.`);
        this.mostrarConfirmEliminar = false;
        this.areaAEliminar = null;
        this.cargarAreas();
      },
      error: (err) => {
        this.toast.remove(loadId);
        const msg = err?.error?.message || 'No se pudo eliminar. Puede estar en uso.';
        this.toast.error('Error al eliminar', msg);
        this.mostrarConfirmEliminar = false;
      }
    });
  }

  cancelarEliminar(): void {
    this.mostrarConfirmEliminar = false;
    this.areaAEliminar = null;
  }

  // =========================
  // CERRAR MODAL
  // =========================
  closeModal(): void {
    this.modalAbierto = false;
    this.submitted = false;
  }
}
