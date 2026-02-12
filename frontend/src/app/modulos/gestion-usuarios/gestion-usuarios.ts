import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';

import { NavbarComponent } from '../../component/navbar';
import { AutoridadAcademicaService } from '../../services/autoridad-academica.service';

// ===== Interfaces segun TU backend =====

interface RolAutoridadDto {
  idRolAutoridad: number;
  nombre: string;
}

interface AutoridadDto {
  idAutoridad: number;
  nombres: string;
  apellidos: string;
  correo: string;
  fechaNacimiento: string; // "2000-02-20"
  estado: boolean;
  idUsuario: number;
  idInstitucion: number;
  rolesAutoridad: RolAutoridadDto[];
}

interface InstitucionDto {
  idInstitucion: number;
  nombreInstitucion: string;
  direccion: string;
  telefono: string;
  estado: boolean;
}

@Component({
  selector: 'app-gestion-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './gestion-usuarios.html',
  styleUrls: ['./gestion-usuarios.scss']
})
export class GestionUsuariosComponent implements OnInit {

  // ===== Tabla =====
  usuarios: AutoridadDto[] = [];
  usuariosFiltrados: AutoridadDto[] = [];

  // ===== Estadísticas =====
  totalUsuarios = 0;
  usuariosActivos = 0;
  totalDecanos = 0;
  totalCoordinadores = 0;

  // ===== Filtros =====
  searchTerm = '';
  filterRole: string = '';   // guardaremos idRolAutoridad como string (por el select)
  filterStatus: string = '';

  // ===== Paginación =====
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  get usuariosPaginados(): AutoridadDto[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.usuariosFiltrados.slice(start, start + this.pageSize);
  }

  // ===== Modales =====
  showCreateModal = false;
  showEditModal = false;
  showRolesModal = false;

  // ===== Formularios =====
  createForm!: FormGroup;

  // ===== Loading =====
  isSaving = false;

  // ===== Data combos =====
  instituciones: InstitucionDto[] = [];
  rolesAutoridadDisponibles: RolAutoridadDto[] = [];

  // Roles seleccionados (ids)
  selectedRolIds: number[] = [];

  // Usuario seleccionado
  selectedUsuario: AutoridadDto | null = null;

  constructor(
    private fb: FormBuilder,
    private autoridadService: AutoridadAcademicaService
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadCatalogs();
    this.loadUsuarios();
  }

  // =========================
  // INIT
  // =========================
  initForms(): void {
    this.createForm = this.fb.group({
      nombres: ['', [Validators.required, Validators.minLength(2)]],
      apellidos: ['', [Validators.required, Validators.minLength(2)]],
      correo: ['', [Validators.required, Validators.email]],
      fechaNacimiento: ['', Validators.required],
      idInstitucion: [null, Validators.required]
    });
  }

  // =========================
  // LOADERS BACKEND
  // =========================
  loadUsuarios(): void {
    this.autoridadService.listarAutoridades().subscribe({
      next: (data) => {
        this.usuarios = Array.isArray(data) ? data : [];
        this.usuariosFiltrados = [...this.usuarios]; // ✅ evita bug del "clic en buscador"
        this.updateStats();
        this.calculatePagination();
      },
      error: (err) => {
        console.error('Error cargando autoridades:', err);
        this.usuarios = [];
        this.usuariosFiltrados = [];
        this.updateStats();
        this.calculatePagination();
      }
    });
  }

  loadCatalogs(): void {
    // instituciones
    this.autoridadService.listarInstituciones().subscribe({
      next: (data) => {
        this.instituciones = Array.isArray(data) ? data : [];
      },
      error: (err) => console.error('Error instituciones:', err)
    });

    // roles
    this.autoridadService.listarCargosAutoridad().subscribe({
      next: (data) => {
        this.rolesAutoridadDisponibles = Array.isArray(data) ? data : [];
      },
      error: (err) => console.error('Error roles autoridad:', err)
    });
  }

  // =========================
  // STATS
  // =========================
  updateStats(): void {
    this.totalUsuarios = this.usuarios.length;
    this.usuariosActivos = this.usuarios.filter(u => u.estado).length;

    this.totalDecanos = this.usuarios.filter(u =>
      (u.rolesAutoridad || []).some(r => this.isDecanato(r.nombre))
    ).length;

    this.totalCoordinadores = this.usuarios.filter(u =>
      (u.rolesAutoridad || []).some(r => this.isCoordinador(r.nombre))
    ).length;
  }

  // =========================
  // FILTERS
  // =========================
  applyFilters(): void {
    const term = (this.searchTerm || '').trim().toLowerCase();
    const statusFilter = this.filterStatus;
    const roleIdFilter = this.filterRole ? Number(this.filterRole) : null;

    this.usuariosFiltrados = this.usuarios.filter(u => {
      const searchMatch =
        !term ||
        (u.nombres || '').toLowerCase().includes(term) ||
        (u.apellidos || '').toLowerCase().includes(term) ||
        (u.correo || '').toLowerCase().includes(term) ||
        String(u.idAutoridad).includes(term);

      const statusMatch =
        !statusFilter || String(u.estado) === statusFilter;

      const roleMatch =
        roleIdFilter == null ||
        (u.rolesAutoridad || []).some(r => r.idRolAutoridad === roleIdFilter);

      return searchMatch && statusMatch && roleMatch;
    });

    this.currentPage = 1;
    this.calculatePagination();
  }

  // =========================
  // PAGINATION
  // =========================
  calculatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.usuariosFiltrados.length / this.pageSize));
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
  // UI HELPERS (roles)
  // =========================
  private norm(s: string): string {
    return (s || '')
      .toUpperCase()
      .replace(/\s+/g, ' ')
      .trim();
  }

  private isDecanato(nombreRol: string): boolean {
    return this.norm(nombreRol).includes('DECANATO');
  }

  private isCoordinador(nombreRol: string): boolean {
    return this.norm(nombreRol).includes('COORDINADOR');
  }

  private isConsejo(nombreRol: string): boolean {
    return this.norm(nombreRol).includes('CONSEJO');
  }

  private isVicerrectorado(nombreRol: string): boolean {
    return this.norm(nombreRol).includes('VICERECTORADO') || this.norm(nombreRol).includes('VICERRECTORADO');
  }

  getRolClass(nombreRol: string): string {
    const n = this.norm(nombreRol);
    if (this.isDecanato(n)) return 'decano';
    if (this.isCoordinador(n)) return 'coordinador';
    if (this.isVicerrectorado(n)) return 'vicerrectorado';
    if (this.isConsejo(n)) return 'consejo';
    return '';
  }

  getRolLabel(nombreRol: string): string {
    const n = this.norm(nombreRol);
    if (this.isDecanato(n)) return 'Decanato';
    if (this.isCoordinador(n)) return 'Coordinador';
    if (this.isVicerrectorado(n)) return 'Vicerrectorado';
    if (this.isConsejo(n)) return 'Consejo';
    return nombreRol;
  }

  getInstitucionNombre(idInstitucion: number): string {
    const inst = this.instituciones.find(i => i.idInstitucion === idInstitucion);
    return inst?.nombreInstitucion || '';
  }

  // =========================
  // TOGGLE STATUS
  // =========================
  toggleStatus(usuario: any): void {
    const nuevoEstado = !usuario.estado;
    const accion = nuevoEstado ? 'activar' : 'desactivar';

    if (!confirm(`¿Seguro que deseas ${accion} esta autoridad?`)) return;

    // Optimistic UI (opcional): cambia en pantalla y revierte si falla
    const estadoAnterior = usuario.estado;
    usuario.estado = nuevoEstado;

    this.autoridadService.cambiarEstadoAutoridad(usuario.idAutoridad, nuevoEstado).subscribe({
      next: () => {
        // refresca stats/filtrado sin recargar todo
        this.updateStats();
        this.applyFilters();
      },
      error: (err) => {
        console.error('Error cambiando estado:', err);
        usuario.estado = estadoAnterior; // revertir
        alert('❌ No se pudo cambiar el estado. Revisa permisos/seguridad.');
      }
    });
  }

  // =========================
  // MODAL CREAR
  // =========================
  openCreateModal(): void {
    this.createForm.reset();
    this.selectedRolIds = [];
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  onRoleToggle(idRolAutoridad: number, event: any): void {
    const checked = !!event?.target?.checked;

    if (checked) {
      if (!this.selectedRolIds.includes(idRolAutoridad)) {
        this.selectedRolIds.push(idRolAutoridad);
      }
    } else {
      this.selectedRolIds = this.selectedRolIds.filter(x => x !== idRolAutoridad);
    }
  }

  saveAutoridad(): void {
    if (this.createForm.invalid) {
      Object.keys(this.createForm.controls).forEach(key => {
        this.createForm.get(key)?.markAsTouched();
      });
      alert('Completa los campos obligatorios.');
      return;
    }

    if (!this.selectedRolIds.length) {
      alert('Selecciona al menos un rol de autoridad.');
      return;
    }

    const v = this.createForm.value;

    // Payload EXACTO para tu backend (AutoridadRegistroRequestDTO)
    const payload = {
      nombres: v.nombres,
      apellidos: v.apellidos,
      correo: v.correo,
      fechaNacimiento: v.fechaNacimiento,
      idInstitucion: v.idInstitucion,
      idsRolAutoridad: this.selectedRolIds
    };

    this.isSaving = true;

    this.autoridadService.registrarAutoridad(payload).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.closeCreateModal();
        alert(`✅ Autoridad registrada.\n\nID Autoridad: ${res?.idAutoridad}\nUsuario App: ${res?.usuarioApp}\nUsuario BD: ${res?.usuarioBd}`);

        // recargar tabla real
        this.loadUsuarios();
      },
      error: (err) => {
        this.isSaving = false;
        console.error('Error registrar autoridad:', err);
        const msg = err?.error?.message || err?.error || err?.message || 'Error desconocido';
        alert('❌ No se pudo registrar: ' + msg);
      }
    });
  }

  // =========================
  // MODAL EDITAR / ROLES (UI)
  // =========================
  openEditModal(usuario: AutoridadDto): void {
    this.selectedUsuario = usuario;
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUsuario = null;
  }

  openRolesModal(usuario: AutoridadDto): void {
    this.selectedUsuario = usuario;
    this.showRolesModal = true;
  }

  closeRolesModal(): void {
    this.showRolesModal = false;
    this.selectedUsuario = null;
  }
}
