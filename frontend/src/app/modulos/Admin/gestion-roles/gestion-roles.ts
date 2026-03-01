// src/app/modulos/Admin/gestion-roles/gestion-roles.ts
//
// Reemplaza roles-autoridad/roles-autoridad.ts
// Gestión de roles_app ↔ roles_bd

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../../../component/navbar';
import {
  RolesAppService,
  RolAppConRolesBdDTO,
  RolAppSavePayload
} from '../../../services/roles-app.service';

@Component({
  selector: 'app-gestion-roles',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './gestion-roles.html',
  styleUrls: ['./gestion-roles.scss']
})
export class GestionRolesComponent implements OnInit {

  // ─── Tabla ──────────────────────────────────────────────────
  roles: RolAppConRolesBdDTO[] = [];
  rolesFiltrados: RolAppConRolesBdDTO[] = [];

  // ─── Catálogo BD ────────────────────────────────────────────
  rolesBdDisponibles: string[] = [];
  selectedRolesBd: string[] = [];

  // ─── Filtros ────────────────────────────────────────────────
  searchTerm = '';
  filterActivo = '';

  // ─── Paginación ─────────────────────────────────────────────
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  // ─── Módulos ─────────────────────────────────────────────────────
  modulosDisponibles: any[] = [];
  moduloSeleccionado: number | null = null;


  get rolesPaginados(): RolAppConRolesBdDTO[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.rolesFiltrados.slice(start, start + this.pageSize);
  }

  // ─── Stats para tarjetas ────────────────────────────────────
  get rolesActivos(): number {
    return this.roles.filter(r => r.activo).length;
  }

  get rolesConBd(): number {
    return this.roles.filter(r => r.rolesBd && r.rolesBd.length > 0).length;
  }

  // ─── Modales ────────────────────────────────────────────────
  showFormModal = false;
  editando = false;
  selectedRol: RolAppConRolesBdDTO | null = null;
  isSaving = false;

  // ─── Form ───────────────────────────────────────────────────
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private svc: RolesAppService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadRolesBd();
    this.loadModulos();
    this.loadData();
  }

  initForm(): void {
    this.form = this.fb.group({
      nombre: ['', [Validators.required, Validators.minLength(2)]],
      descripcion: [''],
      activo: [true]
    });
  }

  // ─── Loaders ────────────────────────────────────────────────

  loadData(): void {
    this.svc.listar().subscribe({
      next: data => {
        this.roles = Array.isArray(data) ? data : [];
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: err => console.error('Error cargando roles_app:', err)
    });
  }

  loadRolesBd(): void {
    this.svc.listarRolesBdDisponibles().subscribe({
      next: data => {
        this.rolesBdDisponibles = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: err => console.error('Error cargando roles BD:', err)
    });
  }
  loadModulos(): void {
    this.svc.listarModulos().subscribe({
      next: data => {
        this.modulosDisponibles = Array.isArray(data) ? data : [];
        this.cdr.detectChanges();
      },
      error: err => console.error('Error cargando módulos:', err)
    });
  }

  // ─── Filtros ────────────────────────────────────────────────

  applyFilters(): void {
    const term = (this.searchTerm || '').trim().toLowerCase();
    this.rolesFiltrados = this.roles.filter(r => {
      const searchMatch = !term
        || (r.nombre || '').toLowerCase().includes(term)
        || (r.descripcion || '').toLowerCase().includes(term);
      const activoMatch = !this.filterActivo
        || String(r.activo) === this.filterActivo;
      return searchMatch && activoMatch;
    });
    this.currentPage = 1;
    this.totalPages = Math.max(1, Math.ceil(this.rolesFiltrados.length / this.pageSize));
  }

  // ─── Paginación ─────────────────────────────────────────────

  changePage(p: number): void {
    if (p >= 1 && p <= this.totalPages) this.currentPage = p;
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 1; i <= Math.min(this.totalPages, 5); i++) pages.push(i);
    return pages;
  }

  // ─── Modal crear ────────────────────────────────────────────

  openCreateModal(): void {
    this.editando = false;
    this.selectedRol = null;
    this.selectedRolesBd = [];
    this.moduloSeleccionado = null;
    this.form.reset({ nombre: '', descripcion: '', activo: true });
    this.showFormModal = true;
    this.cdr.detectChanges();
  }

  // ─── Modal editar ───────────────────────────────────────────

  openEditModal(rol: RolAppConRolesBdDTO): void {
    this.editando = true;
    this.selectedRol = rol;
    this.selectedRolesBd = [...(rol.rolesBd || [])];
    this.moduloSeleccionado = rol.idModulo ?? null;
    this.form.reset({ nombre: rol.nombre, descripcion: rol.descripcion, activo: rol.activo });
    this.showFormModal = true;
    this.cdr.detectChanges();
  }

  closeFormModal(): void {
    this.showFormModal = false;
    this.selectedRol = null;
  }

  // ─── Checkbox roles BD ──────────────────────────────────────

  isRolBdSelected(rolBd: string): boolean {
    return this.selectedRolesBd.includes(rolBd);
  }

  toggleRolBd(rolBd: string, event: any): void {
    const checked = !!event?.target?.checked;
    if (checked) {
      if (!this.selectedRolesBd.includes(rolBd)) this.selectedRolesBd.push(rolBd);
    } else {
      this.selectedRolesBd = this.selectedRolesBd.filter(r => r !== rolBd);
    }
    this.cdr.detectChanges();
  }

  // ─── Guardar ────────────────────────────────────────────────

  save(): void {
    if (this.form.invalid) {
      Object.keys(this.form.controls).forEach(k => this.form.get(k)?.markAsTouched());
      alert('Completa los campos obligatorios.');
      return;
    }

    const v = this.form.value;
    const payload: RolAppSavePayload = {
      nombre: v.nombre,
      descripcion: v.descripcion,
      activo: v.activo,
      rolesBd: [...this.selectedRolesBd],
      idModulo: this.moduloSeleccionado
    };

    this.isSaving = true;

    const obs$ = this.editando && this.selectedRol
      ? this.svc.actualizar(this.selectedRol.idRolApp, payload)
      : this.svc.crear(payload);

    obs$.subscribe({
      next: () => {
        this.isSaving = false;
        this.closeFormModal();
        this.loadData();
        alert(' Rol guardado correctamente.');
      },
      error: err => {
        this.isSaving = false;
        console.error('Error guardando rol:', err);
        alert(' No se pudo guardar el rol: ' + (err?.error?.message || err?.message || 'Error desconocido'));
      }
    });
  }

  // ─── Toggle activo desde tabla ───────────────────────────────

  toggleEstado(rol: RolAppConRolesBdDTO): void {
    const nuevoEstado = !rol.activo;
    if (!confirm(`¿${nuevoEstado ? 'Activar' : 'Desactivar'} el rol "${rol.nombre}"?`)) return;
    const prev = rol.activo;
    rol.activo = nuevoEstado;
    this.svc.cambiarEstado(rol.idRolApp, nuevoEstado).subscribe({
      next: () => this.cdr.detectChanges(),
      error: err => {
        rol.activo = prev;
        console.error('Error cambiando estado:', err);
        alert(' No se pudo cambiar el estado.');
        this.cdr.detectChanges();
      }
    });
  }
}
