import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FooterComponent } from '../../../component/footer.component';
import { NavbarComponent } from '../../../component/navbar';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';
import { CarreraService } from '../../../services/carrera.service';
import { FacultadService } from '../../../services/facultad.service';
@Component({
  selector: 'app-carrera',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, ToastComponent],
  templateUrl: './carrera.html',
  styleUrls: ['./carrera.scss']
})
export class CarreraComponent implements OnInit {

  // ===== Datos =====
  carreras: any[] = [];
  carrerasFiltradas: any[] = [];
  facultades: any[] = [];

  // ===== Filtros =====
  search = '';
  filtroFacultad = '';
  filtroModalidad = '';
  filtroEstado = '';

  // ===== Paginación =====
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  get carrerasPaginadas(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.carrerasFiltradas.slice(start, start + this.pageSize);
  }

  // ===== Estadísticas =====
  get carrerasActivas(): number {
    return this.carreras.filter(c => c.estado).length;
  }

  get totalModalidades(): number {
    if (!this.carreras || this.carreras.length === 0) return 0;
    return new Set(this.carreras.map(c => c.modalidad)).size;
  }

  // ===== Modales =====
  modalAbierto = false;
  editando = false;
  isSaving = false;
  submitted = false;

  // ===== Formulario =====
  form = {
    id: 0,
    idFacultad: null as number | null,
    nombreCarrera: '',
    modalidad: 'presencial',
    estado: true
  };

  constructor(
    private carreraService: CarreraService,
    private facultadService: FacultadService,
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarFacultades();
    this.cargarCarreras();
  }

  // =========================
  // LOADERS BACKEND
  // =========================
  cargarCarreras(): void {
    this.carreraService.getAll().subscribe({
      next: (data) => {
        this.carreras = Array.isArray(data) ? data : [];
        this.carrerasFiltradas = [...this.carreras];
        this.calculatePagination();
        this.cdr.detectChanges();
      },
      error: () => {
        this.carreras = [];
        this.carrerasFiltradas = [];
        this.calculatePagination();
        this.toast.error('Error', 'No se pudieron cargar las carreras.');
      }
    });
  }

  cargarFacultades(): void {
    this.facultadService.listar().subscribe({
      next: (data: any[]) => {
        this.facultades = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: () => {
        this.facultades = [];
        this.toast.error('Error', 'No se pudieron cargar las facultades.');
      }
    });
  }
  getNombreFacultad(idFacultad: number | null | undefined): string {
    if (!idFacultad) return '—';

    const f = this.facultades.find(x => x.idFacultad === idFacultad);
    return f ? f.nombreFacultad : 'No encontrada';
  }
  // =========================
  // FILTROS
  // =========================
  applyFilters(): void {
    const term = (this.search || '').trim().toLowerCase();
    const facultadFilter = this.filtroFacultad;
    const modalidadFilter = this.filtroModalidad;
    const estadoFilter = this.filtroEstado;

    this.carrerasFiltradas = this.carreras.filter(c => {
      // Búsqueda por texto
      const nombre = c.nombre || c.nombreCarrera || '';
      const searchMatch =
        !term ||
        nombre.toLowerCase().includes(term) ||
        String(c.id || c.idCarrera).includes(term);

      // Filtro por facultad
      const nombreFacultad = c.facultad?.nombreFacultad || c.facultad || '';
      const facultadMatch =
        !facultadFilter || nombreFacultad === facultadFilter;

      // Filtro por modalidad
      const modalidadMatch =
        !modalidadFilter || c.modalidad === modalidadFilter;

      // Filtro por estado
      const estadoMatch =
        !estadoFilter || String(c.estado) === estadoFilter;

      return searchMatch && facultadMatch && modalidadMatch && estadoMatch;
    });

    this.currentPage = 1;
    this.calculatePagination();
  }

  // =========================
  // PAGINACIÓN
  // =========================
  calculatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.carrerasFiltradas.length / this.pageSize));
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
  getModalidadClass(modalidad: string): string {
    const m = (modalidad || '').toLowerCase();
    if (m === 'presencial') return 'presencial';
    if (m === 'virtual') return 'virtual';
    if (m === 'hibrido' || m === 'híbrido') return 'hibrido';
    return '';
  }

  getModalidadLabel(modalidad: string): string {
    const m = (modalidad || '').toLowerCase();
    if (m === 'presencial') return 'Presencial';
    if (m === 'virtual') return 'Virtual';
    if (m === 'hibrido' || m === 'híbrido') return 'Híbrido';
    return modalidad;
  }

  // =========================
  // MODAL CREAR
  // =========================
  openCreate(): void {
    this.editando = false;
    this.submitted = false;
    this.form = {
      id: 0,
      idFacultad: this.facultades.length > 0 ? this.facultades[0].idFacultad : null,
      nombreCarrera: '',
      modalidad: 'presencial',
      estado: true
    };
    this.modalAbierto = true;
  }

  // =========================
  // MODAL EDITAR
  // =========================
  edit(carrera: any): void {
    this.editando = true;
    this.submitted = false;
    this.form = {
      id: carrera.id || carrera.idCarrera,
      idFacultad: carrera.facultad?.idFacultad || carrera.idFacultad,
      nombreCarrera: carrera.nombre || carrera.nombreCarrera,
      modalidad: carrera.modalidad,
      estado: carrera.estado
    };
    this.modalAbierto = true;
  }

  // =========================
  // GUARDAR
  // =========================
  guardar(): void {
    this.submitted = true;

    if (!this.form.nombreCarrera?.trim()) {
      this.toast.warning('Campo requerido', 'El nombre de la carrera es obligatorio.');
      return;
    }
    if (!this.form.idFacultad) {
      this.toast.warning('Campo requerido', 'Debe seleccionar una facultad.');
      return;
    }

    this.isSaving = true;
    const nombre = this.form.nombreCarrera.trim();
    const loadId = this.toast.loading(this.editando ? 'Actualizando carrera...' : 'Creando carrera...');

    const request = this.editando
      ? this.carreraService.update(this.form.id, { ...this.form })
      : this.carreraService.create({ ...this.form });

    request.subscribe({
      next: () => {
        this.isSaving = false;
        this.toast.remove(loadId);
        this.toast.success(
          this.editando ? 'Carrera actualizada' : 'Carrera creada',
          `"${nombre}" fue ${this.editando ? 'actualizada' : 'creada'} correctamente.`
        );
        this.closeModal();
        this.cargarCarreras();
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
  // TOGGLE ESTADO
  // =========================
  toggleEstado(carrera: any): void {
    const id = carrera.id || carrera.idCarrera;
    const estadoAnterior = carrera.estado;
    const nuevoEstado = !carrera.estado;
    carrera.estado = nuevoEstado;

    this.carreraService.toggleEstado(id).subscribe({
      next: () => {
        this.toast.info(
          nuevoEstado ? 'Carrera activada' : 'Carrera desactivada',
          `La carrera fue ${nuevoEstado ? 'activada' : 'desactivada'} correctamente.`
        );
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: (err) => {
        carrera.estado = estadoAnterior;
        const msg = err?.error?.message || 'No se pudo cambiar el estado.';
        this.toast.error('Error', msg);
        this.cdr.detectChanges();
      }
    });
  }

  // =========================
  // CERRAR MODAL
  // =========================
  closeModal(): void {
    this.modalAbierto = false;
    this.submitted = false;
  }
}
