// gestion-usuarios.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { NavbarComponent } from '../../component/navbar';
import {
  AutoridadAcademicaService,
  AutoridadResponseDto,
  AutoridadRegistroPayload,
  AutoridadUpdatePayload,
  InstitucionDto,
  RolAutoridadDto
} from '../../services/autoridad-academica.service';

interface RolPreview {
  nombre: string;
  descripcion: string;
}

@Component({
  selector: 'app-gestion-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './gestion-usuarios.html',
  styleUrls: ['./gestion-usuarios.scss']
})
export class GestionUsuariosComponent implements OnInit {

  // data
  usuarios: AutoridadResponseDto[] = [];
  usuariosFiltrados: AutoridadResponseDto[] = [];

  // catalogs
  instituciones: InstitucionDto[] = [];
  rolesAutoridadDisponibles: RolAutoridadDto[] = [];

  // stats
  totalUsuarios = 0;
  usuariosActivos = 0;
  totalDecanos = 0;
  totalCoordinadores = 0;

  // filtros
  searchTerm = '';
  filterRoleId: number | null = null;
  filterStatus = '';

  // paginación
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  // modals
  showCreateModal = false;
  showEditModal = false;
  showRolesModal = false;

  // forms
  createForm!: FormGroup;
  editForm!: FormGroup;

  // states
  isSaving = false;
  isUpdating = false;

  // selected
  selectedUsuario: AutoridadResponseDto | null = null;

  // errors
  rolesAutoridadErrorCreate = false;
  rolesAutoridadErrorEdit = false;

  // preview
  previewRoles: RolPreview[] = [];

  constructor(
    private fb: FormBuilder,
    private autoridadService: AutoridadAcademicaService
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadCatalogs();
    this.loadData();
  }

  // ==========================================
  // INIT FORMS
  // ==========================================
  initForms(): void {
    this.createForm = this.fb.group({
      nombres: ['', [Validators.required, Validators.minLength(2)]],
      apellidos: ['', [Validators.required, Validators.minLength(2)]],
      correo: ['', [Validators.required, Validators.email]],
      fechaNacimiento: ['', Validators.required],
      idInstitucion: [null, Validators.required],
      rolesAutoridad: this.fb.array([]) // boolean array
    });

    this.editForm = this.fb.group({
      nombres: ['', [Validators.required, Validators.minLength(2)]],
      apellidos: ['', [Validators.required, Validators.minLength(2)]],
      correo: ['', [Validators.required, Validators.email]],
      fechaNacimiento: ['', Validators.required],
      idInstitucion: [null, Validators.required],
      rolesAutoridad: this.fb.array([]), // boolean array
      estado: [true, Validators.required]
    });
  }

  private resetRolesArray(form: FormGroup): void {
    const arr = form.get('rolesAutoridad') as FormArray;
    arr.clear();
    this.rolesAutoridadDisponibles.forEach(() => arr.push(this.fb.control(false)));
  }

  // ==========================================
  // LOAD
  // ==========================================
  loadCatalogs(): void {
    this.autoridadService.listarInstituciones().subscribe({
      next: (data) => (this.instituciones = data),
      error: (err) => console.error('Error instituciones', err)
    });

    this.autoridadService.listarCargosAutoridad().subscribe({
      next: (roles) => {
        this.rolesAutoridadDisponibles = roles;
        this.resetRolesArray(this.createForm);
        this.resetRolesArray(this.editForm);
      },
      error: (err) => console.error('Error roles autoridad', err)
    });
  }

  loadData(): void {
    this.autoridadService.listarAutoridades().subscribe({
      next: (data) => {
        this.usuarios = data;
        this.usuariosFiltrados = [...this.usuarios];
        this.updateStats();
        this.calculatePagination();
      },
      error: (err) => console.error('Error listar autoridades', err)
    });
  }

  // ==========================================
  // STATS
  // ==========================================
  updateStats(): void {
    this.totalUsuarios = this.usuarios.length;
    this.usuariosActivos = this.usuarios.filter(u => u.estado).length;

    // si tus nombres de rol son distintos, esto igual funciona por "includes"
    const hasByName = (u: AutoridadResponseDto, text: string) =>
      (u.rolesAutoridad || []).some(r => (r.nombre || '').toLowerCase().includes(text));

    this.totalDecanos = this.usuarios.filter(u => hasByName(u, 'decano')).length;
    this.totalCoordinadores = this.usuarios.filter(u => hasByName(u, 'coordin')).length;
  }

  // ==========================================
  // FILTERS
  // ==========================================
  applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();

    this.usuariosFiltrados = this.usuarios.filter(u => {
      const searchMatch =
        !term ||
        u.nombres.toLowerCase().includes(term) ||
        u.apellidos.toLowerCase().includes(term) ||
        u.correo.toLowerCase().includes(term);

      const roleMatch =
        this.filterRoleId == null ||
        (u.rolesAutoridad || []).some(r => r.idRolAutoridad === this.filterRoleId);

      const statusMatch =
        !this.filterStatus || u.estado.toString() === this.filterStatus;

      return searchMatch && roleMatch && statusMatch;
    });

    this.currentPage = 1;
    this.calculatePagination();
  }

  // ==========================================
  // PAGINATION
  // ==========================================
  calculatePagination(): void {
    this.totalPages = Math.max(1, Math.ceil(this.usuariosFiltrados.length / this.pageSize));
  }

  get usuariosPaginados(): AutoridadResponseDto[] {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    return this.usuariosFiltrados.slice(start, end);
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

  // ==========================================
  // UI HELPERS
  // ==========================================
  getNombreInstitucion(idInstitucion: number): string {
    return this.instituciones.find(i => i.idInstitucion === idInstitucion)?.nombre || '';
  }

  getRolClass(nombre: string): string {
    const n = (nombre || '').toLowerCase();
    if (n.includes('decano')) return 'decano';
    if (n.includes('coordin')) return 'coordinador';
    if (n.includes('vicer')) return 'vicerrectorado';
    if (n.includes('consejo')) return 'consejo';
    return '';
  }

  // ==========================================
  // ROLES SELECTED (FormArray -> ids)
  // ==========================================
  private getSelectedIdsRol(form: FormGroup): number[] {
    const arr = form.get('rolesAutoridad') as FormArray;
    return arr.controls
      .map((c, i) => (c.value ? this.rolesAutoridadDisponibles[i]?.idRolAutoridad : null))
      .filter((x): x is number => x !== null && x !== undefined);
  }

  private setSelectedRolesInForm(form: FormGroup, ids: number[]): void {
    const arr = form.get('rolesAutoridad') as FormArray;
    arr.controls.forEach((c, i) => {
      const id = this.rolesAutoridadDisponibles[i]?.idRolAutoridad;
      c.setValue(ids.includes(id));
    });
  }

  // ==========================================
  // CREATE
  // ==========================================
  openCreateModal(): void {
    this.rolesAutoridadErrorCreate = false;
    this.previewRoles = [];
    this.createForm.reset({
      nombres: '',
      apellidos: '',
      correo: '',
      fechaNacimiento: '',
      idInstitucion: null
    });

    const arr = this.createForm.get('rolesAutoridad') as FormArray;
    arr.controls.forEach(c => c.setValue(false));

    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  onCreateRolesChanged(): void {
    const ids = this.getSelectedIdsRol(this.createForm);
    this.rolesAutoridadErrorCreate = ids.length === 0;

    // Preview simple (sin inventar roles usuario)
    this.previewRoles = ids.length
      ? [{ nombre: 'Asignación automática', descripcion: 'El backend asigna roles de usuario según los cargos.' }]
      : [];
  }

  saveAutoridad(): void {
    if (this.createForm.invalid) {
      Object.keys(this.createForm.controls).forEach(k => this.createForm.get(k)?.markAsTouched());
      return;
    }

    const idsRolAutoridad = this.getSelectedIdsRol(this.createForm);
    this.rolesAutoridadErrorCreate = idsRolAutoridad.length === 0;
    if (this.rolesAutoridadErrorCreate) return;

    const v = this.createForm.value;

    const payload: AutoridadRegistroPayload = {
      nombres: (v.nombres ?? '').trim(),
      apellidos: (v.apellidos ?? '').trim(),
      correo: (v.correo ?? '').trim(),
      fechaNacimiento: v.fechaNacimiento,
      idInstitucion: Number(v.idInstitucion),
      idsRolAutoridad
    };

    this.isSaving = true;
    this.autoridadService.registrarAutoridad(payload).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.closeCreateModal();

        alert(
          `✅ Autoridad registrada\n\n` +
          `ID Autoridad: ${res.idAutoridad}\n` +
          `Usuario App: ${res.usuarioApp}\n` +
          `Usuario BD: ${res.usuarioBd}\n\n` +
          `Se enviaron credenciales al correo.`
        );

        this.loadData();
      },
      error: (err) => {
        this.isSaving = false;
        const msg = err?.error?.message || err?.error || err?.message || 'Error al registrar';
        alert('❌ ' + msg);
      }
    });
  }

  // ==========================================
  // EDIT
  // ==========================================
  openEditModal(usuario: AutoridadResponseDto): void {
    this.selectedUsuario = usuario;
    this.rolesAutoridadErrorEdit = false;

    this.editForm.patchValue({
      nombres: usuario.nombres,
      apellidos: usuario.apellidos,
      correo: usuario.correo,
      fechaNacimiento: usuario.fechaNacimiento,
      idInstitucion: usuario.idInstitucion,
      estado: usuario.estado
    });

    const ids = (usuario.rolesAutoridad || []).map(r => r.idRolAutoridad);
    this.setSelectedRolesInForm(this.editForm, ids);

    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUsuario = null;
  }

  onEditRolesChanged(): void {
    const ids = this.getSelectedIdsRol(this.editForm);
    this.rolesAutoridadErrorEdit = ids.length === 0;
  }

  updateAutoridad(): void {
    if (!this.selectedUsuario) return;

    if (this.editForm.invalid) return;

    const idsRolAutoridad = this.getSelectedIdsRol(this.editForm);
    this.rolesAutoridadErrorEdit = idsRolAutoridad.length === 0;
    if (this.rolesAutoridadErrorEdit) return;

    const v = this.editForm.value;

    const payload: AutoridadUpdatePayload = {
      nombres: (v.nombres ?? '').trim(),
      apellidos: (v.apellidos ?? '').trim(),
      correo: (v.correo ?? '').trim(),
      fechaNacimiento: v.fechaNacimiento,
      estado: !!v.estado,
      idUsuario: this.selectedUsuario.idUsuario,       // NO se cambia
      idInstitucion: Number(v.idInstitucion),
      idsRolAutoridad
    };

    this.isUpdating = true;

    this.autoridadService.actualizarAutoridad(this.selectedUsuario.idAutoridad, payload).subscribe({
      next: () => {
        this.isUpdating = false;
        this.closeEditModal();
        alert('✅ Autoridad actualizada');
        this.loadData();
      },
      error: (err) => {
        this.isUpdating = false;
        const msg = err?.error?.message || err?.error || err?.message || 'Error al actualizar';
        alert('❌ ' + msg);
      }
    });
  }

  // ==========================================
  // TOGGLE STATUS (usa PUT cambiando autoridad.estado)
  // ==========================================
  toggleStatus(usuario: AutoridadResponseDto): void {
    const nuevoEstado = !usuario.estado;

    // armamos el payload completo (tu backend lo requiere)
    const payload: AutoridadUpdatePayload = {
      nombres: usuario.nombres,
      apellidos: usuario.apellidos,
      correo: usuario.correo,
      fechaNacimiento: usuario.fechaNacimiento,
      estado: nuevoEstado,
      idUsuario: usuario.idUsuario,
      idInstitucion: usuario.idInstitucion,
      idsRolAutoridad: (usuario.rolesAutoridad || []).map(r => r.idRolAutoridad)
    };

    this.autoridadService.actualizarAutoridad(usuario.idAutoridad, payload).subscribe({
      next: () => {
        usuario.estado = nuevoEstado;
        this.updateStats();
        this.applyFilters();
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || err?.message || 'No se pudo actualizar el estado';
        alert('❌ ' + msg);
      }
    });
  }

  // ==========================================
  // DELETE
  // ==========================================
  deleteAutoridad(usuario: AutoridadResponseDto): void {
    if (!confirm(`¿Eliminar autoridad ${usuario.nombres} ${usuario.apellidos}?`)) return;

    this.autoridadService.eliminarAutoridad(usuario.idAutoridad).subscribe({
      next: () => {
        alert('✅ Eliminado');
        this.loadData();
      },
      error: (err) => {
        const msg = err?.error?.message || err?.error || err?.message || 'No se pudo eliminar';
        alert('❌ ' + msg);
      }
    });
  }

  // ==========================================
  // ROLES MODAL (solo visualizar cargos reales)
  // ==========================================
  openRolesModal(usuario: AutoridadResponseDto): void {
    this.selectedUsuario = usuario;
    this.showRolesModal = true;
  }

  closeRolesModal(): void {
    this.showRolesModal = false;
    this.selectedUsuario = null;
  }
}
