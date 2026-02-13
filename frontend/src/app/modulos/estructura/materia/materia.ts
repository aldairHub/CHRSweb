import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import { MateriaService } from '../../../services/materia.service';
import { CarreraService } from '../../../services/carrera.service';

interface Materia {
  id?: number;
  carrera?: string;
  carreraId?: number | null;
  nombre: string;
  nivel: number;
}

interface Carrera {
  id?: number;
  idCarrera?: number;
  nombre?: string;
  nombreCarrera?: string;
}

@Component({
  selector: 'app-materia',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NavbarComponent
  ],
  templateUrl: './materia.html',
  styleUrls: ['./materia.scss']
})
export class MateriaComponent implements OnInit {

  // ===== Datos =====
  materias: Materia[] = [];
  materiasFiltradas: Materia[] = [];
  carreras: Carrera[] = [];

  // ===== Filtros =====
  search = '';
  filtroCarrera = '';
  filtroNivel: number | string = '';

  // ===== Paginación =====
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  get materiasPaginadas(): Materia[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.materiasFiltradas.slice(start, start + this.pageSize);
  }

  // ===== Estadísticas =====
  get nivelesUnicos(): number {
    if (!this.materias || this.materias.length === 0) return 0;
    return new Set(this.materias.map(m => m.nivel)).size;
  }

  get carrerasUnicas(): string[] {
    const carreras = new Set(
      this.materias
        .map(m => m.carrera)
        .filter((c): c is string => c !== undefined && c !== null)
    );
    return Array.from(carreras).sort();
  }

  get nivelesDisponibles(): number[] {
    const niveles = new Set(this.materias.map(m => m.nivel));
    return Array.from(niveles).sort((a, b) => a - b);
  }

  // ===== Modales =====
  modalAbierto = false;
  editando = false;
  isSaving = false;
  submitted = false;

  // ===== Formulario =====
  form: Materia = {
    id: 0,
    carreraId: null,
    nombre: '',
    nivel: 1
  };

  constructor(
    private materiaService: MateriaService,
    private carreraService: CarreraService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarCarreras();
    this.cargarMaterias();
  }

  // =========================
  // LOADERS BACKEND
  // =========================
  cargarCarreras(): void {
    this.carreraService.getAll().subscribe({
      next: (data: any) => {
        this.carreras = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error cargando carreras:', err);
        this.carreras = [];
      }
    });
  }

  cargarMaterias(): void {
    this.materiaService.listar().subscribe({
      next: (data: any[]) => {

        this.materias = data.map(m => ({
          id: m.idMateria,
          nombre: m.nombre,
          nivel: m.nivel,
          carreraId: m.idCarrera,
          carrera: m.nombreCarrera
        }));

        this.materiasFiltradas = [...this.materias];
        this.calculatePagination();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error de conexión:', err);
        this.materias = [];
        this.materiasFiltradas = [];
        this.calculatePagination();
        this.cdr.detectChanges();
      }
    });
  }


  // =========================
  // FILTROS
  // =========================
  applyFilters(): void {
    const term = (this.search || '').trim().toLowerCase();
    const carreraFilter = this.filtroCarrera;
    const nivelFilter = this.filtroNivel;

    this.materiasFiltradas = this.materias.filter(m => {

      const searchMatch =
        !term ||
        m.nombre.toLowerCase().includes(term) ||
        (m.carrera?.toLowerCase().includes(term) ?? false) ||
        String(m.id ?? '').includes(term);

      const carreraMatch =
        !carreraFilter || m.carrera === carreraFilter;

      const nivelMatch =
        !nivelFilter || m.nivel === Number(nivelFilter);

      return searchMatch && carreraMatch && nivelMatch;
    });

    this.currentPage = 1;
    this.calculatePagination();
  }

  // =========================
  // PAGINACIÓN
  // =========================
  calculatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.materiasFiltradas.length / this.pageSize));
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
  getCarreraNombre(carreraId?: number | null): string {
    if (!carreraId) return 'Sin carrera';

    const carrera = this.carreras.find(c =>
      (c.id && c.id === carreraId) ||
      (c.idCarrera && c.idCarrera === carreraId)
    );

    return carrera
      ? (carrera.nombre || carrera.nombreCarrera || 'Sin nombre')
      : 'Carrera no encontrada';
  }

  // =========================
  // MODAL CREAR
  // =========================
  openCreate(): void {
    this.editando = false;
    this.submitted = false;
    this.form = {
      id: 0,
      carreraId: this.carreras.length > 0
        ? (this.carreras[0].id || this.carreras[0].idCarrera || null)
        : null,
      nombre: '',
      nivel: 1
    };
    this.modalAbierto = true;
  }

  // =========================
  // MODAL EDITAR
  // =========================
  edit(m: Materia): void {
    this.editando = true;
    this.submitted = false;
    this.form = { ...m };
    this.modalAbierto = true;
  }

  // =========================
  // GUARDAR
  // =========================
  guardar(): void {
    this.submitted = true;

    if (!this.form.nombre?.trim()) {
      alert('El nombre de la materia es obligatorio.');
      return;
    }

    if (this.form.carreraId == null) {
      alert('Debe seleccionar una carrera.');
      return;
    }
    this.isSaving = true;
    console.log('Payload enviado:', {
      nombre: this.form.nombre,
      nivel: this.form.nivel,
      carreraId: this.form.carreraId
    });

    this.materiaService.crear({
      nombre: this.form.nombre.trim(),
      nivel: this.form.nivel,
      carreraId: Number(this.form.carreraId) // ← fuerza número real

    }).subscribe({
      next: () => {
        this.isSaving = false;
        this.closeModal();
        alert('✅ Materia creada con éxito.');
        this.cargarMaterias();
      },
      error: (err) => {
        this.isSaving = false;
        console.error('Error al guardar:', err);
        const msg = err?.error?.message || err?.error || err?.message || 'Error desconocido';
        alert('❌ No se pudo crear: ' + msg);
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
