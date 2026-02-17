// src/app/modulos/gestion-usuarios/gestion-usuarios.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../../component/navbar';
import {
  UsuarioAdminService,
  UsuarioConRolesDTO,
  AutoridadConRolesDTO
} from '../../services/usuario-admin.service';
import { RolesAppService, RolAppDTO } from '../../services/roles-app.service';

@Component({
  selector: 'app-gestion-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './gestion-usuarios.html',
  styleUrls: ['./gestion-usuarios.scss']
})
export class GestionUsuariosComponent implements OnInit {

  // ─── Pestaña activa ─────────────────────────────────────────
  activeTab: 'autoridades' | 'usuarios' = 'autoridades';

  // ─── Datos ──────────────────────────────────────────────────
  autoridades: AutoridadConRolesDTO[] = [];
  autoridadesFiltradas: AutoridadConRolesDTO[] = [];

  usuarios: UsuarioConRolesDTO[] = [];
  usuariosFiltrados: UsuarioConRolesDTO[] = [];

  // ─── Catálogo roles_app para el modal ───────────────────────
  rolesAppDisponibles: RolAppDTO[] = [];
  selectedRolIds: number[] = [];

  // ─── Filtros ────────────────────────────────────────────────
  searchAutoridad = '';
  filterEstadoAutoridad = '';
  searchUsuario = '';
  filterEstadoUsuario = '';

  // ─── Paginación ─────────────────────────────────────────────
  pageAutoridad = 1;
  pageUsuario = 1;
  pageSize = 10;
  Math = Math;

  get autoridadesPaginadas(): AutoridadConRolesDTO[] {
    const s = (this.pageAutoridad - 1) * this.pageSize;
    return this.autoridadesFiltradas.slice(s, s + this.pageSize);
  }

  get totalPagesAutoridad(): number {
    return Math.max(1, Math.ceil(this.autoridadesFiltradas.length / this.pageSize));
  }

  get usuariosPaginados(): UsuarioConRolesDTO[] {
    const s = (this.pageUsuario - 1) * this.pageSize;
    return this.usuariosFiltrados.slice(s, s + this.pageSize);
  }

  get totalPagesUsuario(): number {
    return Math.max(1, Math.ceil(this.usuariosFiltrados.length / this.pageSize));
  }

  // ─── Stats (reemplazan al pipe eliminado) ───────────────────
  get autoridadesActivas(): number {
    return this.autoridades.filter(a => a.estado).length;
  }

  get usuariosActivos(): number {
    return this.usuarios.filter(u => u.activo).length;
  }

  // ─── Modal edición de roles ──────────────────────────────────
  showRolesModal = false;
  rolesModalTipo: 'autoridad' | 'usuario' = 'autoridad';
  selectedAutoridad: AutoridadConRolesDTO | null = null;
  selectedUsuario: UsuarioConRolesDTO | null = null;
  isSaving = false;

  constructor(
    private svc: UsuarioAdminService,
    private rolesSvc: RolesAppService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadRolesApp();
    this.loadAutoridades();
    this.loadUsuarios();
  }

  // ─── Loaders ────────────────────────────────────────────────

  loadRolesApp(): void {
    this.rolesSvc.listar().subscribe({
      next: data => {
        this.rolesAppDisponibles = (Array.isArray(data) ? data : []).map(r => ({
          idRolApp: r.idRolApp,
          nombre: r.nombre,
          descripcion: r.descripcion,
          activo: r.activo,
          fechaCreacion: r.fechaCreacion
        } as RolAppDTO));
        this.cdr.detectChanges();
      },
      error: err => console.error('Error roles_app:', err)
    });
  }

  loadAutoridades(): void {
    this.svc.listarAutoridades().subscribe({
      next: data => {
        this.autoridades = Array.isArray(data) ? data : [];
        this.applyFiltersAutoridades();
        this.cdr.detectChanges();
      },
      error: err => console.error('Error autoridades:', err)
    });
  }

  loadUsuarios(): void {
    this.svc.listarUsuarios().subscribe({
      next: data => {
        this.usuarios = Array.isArray(data) ? data : [];
        this.applyFiltersUsuarios();
        this.cdr.detectChanges();
      },
      error: err => console.error('Error usuarios:', err)
    });
  }

  // ─── Filtros autoridades ─────────────────────────────────────

  applyFiltersAutoridades(): void {
    const t = (this.searchAutoridad || '').trim().toLowerCase();
    this.autoridadesFiltradas = this.autoridades.filter(a => {
      const sm = !t
        || (a.nombres || '').toLowerCase().includes(t)
        || (a.apellidos || '').toLowerCase().includes(t)
        || (a.correo || '').toLowerCase().includes(t);
      const em = !this.filterEstadoAutoridad
        || String(a.estado) === this.filterEstadoAutoridad;
      return sm && em;
    });
    this.pageAutoridad = 1;
    this.cdr.detectChanges();
  }

  // ─── Filtros usuarios ────────────────────────────────────────

  applyFiltersUsuarios(): void {
    const t = (this.searchUsuario || '').trim().toLowerCase();
    this.usuariosFiltrados = this.usuarios.filter(u => {
      const sm = !t
        || (u.usuarioApp || '').toLowerCase().includes(t)
        || (u.correo || '').toLowerCase().includes(t);
      const em = !this.filterEstadoUsuario
        || String(u.activo) === this.filterEstadoUsuario;
      return sm && em;
    });
    this.pageUsuario = 1;
    this.cdr.detectChanges();
  }

  // ─── Toggle estado autoridad ─────────────────────────────────

  toggleEstadoAutoridad(a: AutoridadConRolesDTO): void {
    const nuevo = !a.estado;
    if (!confirm(`¿${nuevo ? 'Activar' : 'Desactivar'} a ${a.nombres} ${a.apellidos}?`)) return;
    const prev = a.estado;
    a.estado = nuevo;
    this.svc.cambiarEstadoAutoridad(a.idAutoridad, nuevo).subscribe({
      next: () => this.cdr.detectChanges(),
      error: err => {
        a.estado = prev;
        console.error(err);
        alert('❌ No se pudo cambiar el estado.');
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Toggle estado usuario ───────────────────────────────────

  toggleEstadoUsuario(u: UsuarioConRolesDTO): void {
    const nuevo = !u.activo;
    if (!confirm(`¿${nuevo ? 'Activar' : 'Desactivar'} al usuario ${u.usuarioApp}?`)) return;
    const prev = u.activo;
    u.activo = nuevo;
    this.svc.cambiarEstadoUsuario(u.idUsuario, nuevo).subscribe({
      next: () => this.cdr.detectChanges(),
      error: err => {
        u.activo = prev;
        console.error(err);
        alert('❌ No se pudo cambiar el estado.');
        this.cdr.detectChanges();
      }
    });
  }

  // ─── Modal roles autoridad ───────────────────────────────────

  openRolesAutoridadModal(a: AutoridadConRolesDTO): void {
    this.rolesModalTipo = 'autoridad';
    this.selectedAutoridad = a;
    this.selectedUsuario = null;
    this.selectedRolIds = (a.rolesApp || []).map(r => r.idRolApp);
    this.showRolesModal = true;
    this.cdr.detectChanges();
  }

  // ─── Modal roles usuario ─────────────────────────────────────

  openRolesUsuarioModal(u: UsuarioConRolesDTO): void {
    this.rolesModalTipo = 'usuario';
    this.selectedUsuario = u;
    this.selectedAutoridad = null;
    this.selectedRolIds = (u.rolesApp || []).map(r => r.idRolApp);
    this.showRolesModal = true;
    this.cdr.detectChanges();
  }

  closeRolesModal(): void {
    this.showRolesModal = false;
    this.selectedAutoridad = null;
    this.selectedUsuario = null;
  }

  toggleRolApp(id: number, ev: any): void {
    const checked = !!ev?.target?.checked;
    if (checked) {
      if (!this.selectedRolIds.includes(id)) this.selectedRolIds.push(id);
    } else {
      this.selectedRolIds = this.selectedRolIds.filter(x => x !== id);
    }
  }

  isRolSelected(id: number): boolean {
    return this.selectedRolIds.includes(id);
  }

  // ─── Guardar roles ───────────────────────────────────────────

  saveRoles(): void {
    this.isSaving = true;
    const ids = [...this.selectedRolIds];

    if (this.rolesModalTipo === 'autoridad' && this.selectedAutoridad) {
      this.svc.actualizarRolesAutoridad(this.selectedAutoridad.idAutoridad, ids).subscribe({
        next: updated => {
          const idx = this.autoridades.findIndex(a => a.idAutoridad === updated.idAutoridad);
          if (idx >= 0) this.autoridades[idx] = updated;
          this.applyFiltersAutoridades();
          this.isSaving = false;
          this.closeRolesModal();
          alert('✅ Roles actualizados.');
        },
        error: err => {
          this.isSaving = false;
          console.error(err);
          alert('❌ Error actualizando roles.');
        }
      });

    } else if (this.rolesModalTipo === 'usuario' && this.selectedUsuario) {
      this.svc.actualizarRolesUsuario(this.selectedUsuario.idUsuario, ids).subscribe({
        next: updated => {
          const idx = this.usuarios.findIndex(u => u.idUsuario === updated.idUsuario);
          if (idx >= 0) this.usuarios[idx] = updated;
          this.applyFiltersUsuarios();
          this.isSaving = false;
          this.closeRolesModal();
          alert('✅ Roles actualizados.');
        },
        error: err => {
          this.isSaving = false;
          console.error(err);
          alert('❌ Error actualizando roles.');
        }
      });
    }
  }

  // ─── Helper paginación ───────────────────────────────────────

  getPageNums(total: number): number[] {
    const arr: number[] = [];
    for (let i = 1; i <= total; i++) arr.push(i);
    return arr;
  }
}
