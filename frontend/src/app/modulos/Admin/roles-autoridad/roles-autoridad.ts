import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import { RolAutoridadRolesService } from '../../../services/rol-autoridad.service';

interface RolUsuarioDto {
  idRolUsuario: number;
  nombre: string;
}

interface RolAutoridadDto {
  idRolAutoridad: number;
  nombre: string;
  rolesUsuario?: RolUsuarioDto[];
}

@Component({
  selector: 'app-roles-autoridad',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './roles-autoridad.html',
  styleUrls: ['./roles-autoridad.scss']
})
export class RolesAutoridadComponent implements OnInit {

  // ===== Tabla =====
  rolesAutoridad: RolAutoridadDto[] = [];
  rolesFiltrados: RolAutoridadDto[] = [];

  // ===== Stats =====
  totalRolesAutoridad = 0;
  totalAsignaciones = 0;
  totalSinRoles = 0;

  // ===== Filtros =====
  searchTerm = '';
  filterHasRoles: string = ''; // "con" | "sin" | ""

  // ===== Paginación =====
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  get rolesPaginados(): RolAutoridadDto[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.rolesFiltrados.slice(start, start + this.pageSize);
  }

  // ===== Modales =====
  showCreateModal = false;
  showEditModal = false;
  showRolesModal = false;

  // ===== Form =====
  form!: FormGroup;
  editando = false;

  // ===== Catálogos =====
  rolesUsuarioDisponibles: RolUsuarioDto[] = [];

  // ===== Selección =====
  selectedRolAutoridad: RolAutoridadDto | null = null;
  selectedRolesUsuarioIds: number[] = [];

  // ===== Loading =====
  isSaving = false;

  constructor(
    private fb: FormBuilder,
    private service: RolAutoridadRolesService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadCatalogs();
    this.loadData();
  }

  initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(3)]]
    });
  }

  // =========================
  // LOADERS
  // =========================
  loadCatalogs(): void {
    this.service.listarRolesUsuario().subscribe({
      next: (data: any) => {
        this.rolesUsuarioDisponibles = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Error cargando roles_usuario:', err)
    });
  }

  loadData(): void {
    // Este endpoint devuelve: [{ idRolAutoridad, nombre, rolesUsuario: [...] }]
    this.service.listarConRolesUsuario().subscribe({
      next: (data: any) => {
        this.rolesAutoridad = Array.isArray(data) ? data : [];
        this.rolesFiltrados = [...this.rolesAutoridad];
        this.updateStats();
        this.calculatePagination();
        this.cdr.detectChanges();
      },
      error: (err: any) => {
        console.error('Error cargando roles_autoridad con roles_usuario:', err);
        this.rolesAutoridad = [];
        this.rolesFiltrados = [];
        this.updateStats();
        this.calculatePagination();
        this.cdr.detectChanges();
      }
    });
  }

  // =========================
  // STATS
  // =========================
  updateStats(): void {
    this.totalRolesAutoridad = this.rolesAutoridad.length;

    this.totalAsignaciones = this.rolesAutoridad.reduce(
      (acc, r) => acc + ((r.rolesUsuario || []).length),
      0
    );

    this.totalSinRoles = this.rolesAutoridad.filter(r => (r.rolesUsuario || []).length === 0).length;
  }

  // =========================
  // FILTERS
  // =========================
  applyFilters(): void {
    const term = (this.searchTerm || '').trim().toLowerCase();
    const hasRolesFilter = this.filterHasRoles; // "con" | "sin" | ""

    this.rolesFiltrados = this.rolesAutoridad.filter(r => {
      const searchMatch =
        !term ||
        (r.nombre || '').toLowerCase().includes(term) ||
        String(r.idRolAutoridad).includes(term);

      const count = (r.rolesUsuario || []).length;
      const hasRolesMatch =
        !hasRolesFilter ||
        (hasRolesFilter === 'con' && count > 0) ||
        (hasRolesFilter === 'sin' && count === 0);

      return searchMatch && hasRolesMatch;
    });

    this.currentPage = 1;
    this.calculatePagination();
  }

  // =========================
  // PAGINATION
  // =========================
  calculatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.rolesFiltrados.length / this.pageSize));
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
  // MODALS
  // =========================
  openCreateModal(): void {
    this.editando = false;
    this.form.reset({ nombre: '' });
    this.selectedRolesUsuarioIds = [];
    this.showCreateModal = true;
    this.cdr.detectChanges();
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  openEditModal(rol: RolAutoridadDto): void {
    this.editando = true;
    this.selectedRolAutoridad = rol;

    this.form.reset({ nombre: rol.nombre });

    this.selectedRolesUsuarioIds = (rol.rolesUsuario || []).map(x => x.idRolUsuario);
    this.showEditModal = true;
    this.cdr.detectChanges();
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedRolAutoridad = null;
  }

  openRolesModal(rol: RolAutoridadDto): void {
    this.selectedRolAutoridad = rol;
    this.showRolesModal = true;
  }

  closeRolesModal(): void {
    this.showRolesModal = false;
    this.selectedRolAutoridad = null;
  }

  // =========================
  // CHECKBOX ROLES_USUARIO
  // =========================
  onRolUsuarioToggle(idRolUsuario: number, event: any): void {
    const checked = !!event?.target?.checked;

    if (checked) {
      if (!this.selectedRolesUsuarioIds.includes(idRolUsuario)) {
        this.selectedRolesUsuarioIds.push(idRolUsuario);
      }
    } else {
      this.selectedRolesUsuarioIds = this.selectedRolesUsuarioIds.filter(x => x !== idRolUsuario);
    }

    this.cdr.detectChanges();
  }

  // =========================
  // SAVE (create/update)
  // =========================
  save(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(k => this.form.get(k)?.markAsTouched());
      alert('Completa los campos obligatorios.');
      return;
    }

    const v = this.form.value;

    const payload = {
      nombre: v.nombre,
      rolesUsuarioIds: [...this.selectedRolesUsuarioIds]
    };

    this.isSaving = true;

    if (!this.editando) {
      this.service.crearRolAutoridad(payload).subscribe({
        next: () => {
          this.isSaving = false;
          this.closeCreateModal();
          this.loadData();
          alert('✅ Rol de autoridad creado.');
        },
        error: (err: any) => {
          this.isSaving = false;
          console.error('Error crear rol_autoridad:', err);
          alert('❌ No se pudo crear el rol.');
        }
      });
      return;
    }

    const id = this.selectedRolAutoridad?.idRolAutoridad;
    if (!id) {
      this.isSaving = false;
      alert('❌ No se pudo identificar el rol a editar.');
      return;
    }

    this.service.actualizarRolAutoridad(id, payload).subscribe({
      next: () => {
        this.isSaving = false;
        this.closeEditModal();
        this.loadData();
        alert('✅ Rol de autoridad actualizado.');
      },
      error: (err: any) => {
        this.isSaving = false;
        console.error('Error actualizar rol_autoridad:', err);
        alert('❌ No se pudo actualizar el rol.');
      }
    });
  }
}
