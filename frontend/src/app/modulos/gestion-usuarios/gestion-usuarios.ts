// src/app/modulos/gestion-usuarios/gestion-usuarios.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NavbarComponent } from '../../component/navbar';

import {
  UsuarioAdminService,
  UsuarioConRolesDTO,
  AutoridadConRolesDTO,
  AutoridadCreatePayload,
  UsuarioCreatePayload
} from '../../services/usuario-admin.service';

import { RolesAppService, RolAppConRolesBdDTO } from '../../services/roles-app.service';

interface InstitucionDTO {
  idInstitucion: number;
  nombreInstitucion: string;
}

@Component({
  selector: 'app-gestion-usuarios',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './gestion-usuarios.html',
  styleUrls: ['./gestion-usuarios.scss']
})
export class GestionUsuariosComponent implements OnInit {

  // --- Pestana activa ------------------------------------------
  activeTab: 'autoridades' | 'usuarios' = 'autoridades';

  // --- Datos ---------------------------------------------------
  autoridades: AutoridadConRolesDTO[] = [];
  autoridadesFiltradas: AutoridadConRolesDTO[] = [];
  usuarios: UsuarioConRolesDTO[] = [];
  usuariosFiltrados: UsuarioConRolesDTO[] = [];

  // --- Catalogos -----------------------------------------------
  rolesAppDisponibles: RolAppConRolesBdDTO[] = [];
  instituciones: InstitucionDTO[] = [];

  // --- Filtros -------------------------------------------------
  searchAutoridad = '';
  filterEstadoAutoridad = '';
  searchUsuario = '';
  filterEstadoUsuario = '';

  // --- Paginacion ----------------------------------------------
  pageAutoridad = 1;
  pageUsuario   = 1;
  pageSize      = 10;
  Math          = Math;

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

  // --- Stats ---------------------------------------------------
  get autoridadesActivas(): number { return this.autoridades.filter(a => a.estado).length; }
  get usuariosActivos():    number { return this.usuarios.filter(u => u.activo).length; }

  // --- Modal edicion de roles ----------------------------------
  showRolesModal    = false;
  rolesModalTipo: 'autoridad' | 'usuario' = 'autoridad';
  selectedAutoridad: AutoridadConRolesDTO | null = null;
  selectedUsuario:   UsuarioConRolesDTO   | null = null;
  selectedRolIds:    number[]             = [];
  isSaving          = false;

  // --- Modal creacion ------------------------------------------
  showCreateModal  = false;
  createModalTipo: 'autoridad' | 'usuario' = 'autoridad';
  isCreating       = false;
  createRolIds:    number[] = [];

  /** Permisos BD de los roles seleccionados en el modal de creacion */
  get permisosBdDeSeleccionados(): string[] {
    const set = new Set<string>();
    this.rolesAppDisponibles
      .filter(r => this.createRolIds.includes(r.idRolApp))
      .forEach(r => (r.rolesBd ?? []).forEach(bd => set.add(bd)));
    return Array.from(set).sort();
  }

  formAutoridad = {
    nombres:          '',
    apellidos:        '',
    correo:           '',
    fechaNacimiento:  '',      // string YYYY-MM-DD del input[type=date]
    idInstitucion:    null as number | null
  };

  formUsuario = {
    nombres:   '',
    apellidos: '',
    correo:    ''
  };

  constructor(
    private svc:      UsuarioAdminService,
    private rolesSvc: RolesAppService,
    private http:     HttpClient,
    private cdr:      ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadRolesApp();
    this.loadInstituciones();
    this.loadAutoridades();
    this.loadUsuarios();
  }

  // --- Loaders ------------------------------------------------

  loadRolesApp(): void {
    this.rolesSvc.listar().subscribe({
      next: data => { this.rolesAppDisponibles = Array.isArray(data) ? data : []; this.cdr.detectChanges(); },
      error: (err: any) => console.error('Error roles_app:', err)
    });
  }

  loadInstituciones(): void {
    this.http.get<InstitucionDTO[]>('http://localhost:8080/api/instituciones').subscribe({
      next: data => {
        this.instituciones = Array.isArray(data) ? data : [];
        if (this.instituciones.length === 1) {
          this.formAutoridad.idInstitucion = this.instituciones[0].idInstitucion;
        }
        this.cdr.detectChanges();
      },
      error: (err: any) => console.error('Error instituciones:', err)
    });
  }

  loadAutoridades(): void {
    this.svc.listarAutoridades().subscribe({
      next: data => { this.autoridades = Array.isArray(data) ? data : []; this.applyFiltersAutoridades(); this.cdr.detectChanges(); },
      error: (err: any) => console.error('Error autoridades:', err)
    });
  }

  loadUsuarios(): void {
    this.svc.listarUsuarios().subscribe({
      next: data => { this.usuarios = Array.isArray(data) ? data : []; this.applyFiltersUsuarios(); this.cdr.detectChanges(); },
      error: (err: any) => console.error('Error usuarios:', err)
    });
  }

  // --- Filtros ------------------------------------------------

  applyFiltersAutoridades(): void {
    const t = (this.searchAutoridad || '').trim().toLowerCase();
    this.autoridadesFiltradas = this.autoridades.filter(a => {
      const sm = !t || (a.nombres||'').toLowerCase().includes(t) || (a.apellidos||'').toLowerCase().includes(t) || (a.correo||'').toLowerCase().includes(t);
      const em = !this.filterEstadoAutoridad || String(a.estado) === this.filterEstadoAutoridad;
      return sm && em;
    });
    this.pageAutoridad = 1;
    this.cdr.detectChanges();
  }

  applyFiltersUsuarios(): void {
    const t = (this.searchUsuario || '').trim().toLowerCase();
    this.usuariosFiltrados = this.usuarios.filter(u => {
      const sm = !t || (u.usuarioApp||'').toLowerCase().includes(t) || (u.correo||'').toLowerCase().includes(t);
      const em = !this.filterEstadoUsuario || String(u.activo) === this.filterEstadoUsuario;
      return sm && em;
    });
    this.pageUsuario = 1;
    this.cdr.detectChanges();
  }

  // --- Toggle estado ------------------------------------------

  toggleEstadoAutoridad(a: AutoridadConRolesDTO): void {
    const nuevo = !a.estado;
    if (!confirm(`¿${nuevo ? 'Activar' : 'Desactivar'} a ${a.nombres} ${a.apellidos}?`)) return;
    const prev = a.estado;
    a.estado = nuevo;
    this.svc.cambiarEstadoAutoridad(a.idAutoridad, nuevo).subscribe({
      next: () => this.cdr.detectChanges(),
      error: (err: any) => { a.estado = prev; console.error(err); alert('No se pudo cambiar el estado.'); this.cdr.detectChanges(); }
    });
  }

  toggleEstadoUsuario(u: UsuarioConRolesDTO): void {
    const nuevo = !u.activo;
    if (!confirm(`¿${nuevo ? 'Activar' : 'Desactivar'} al usuario ${u.usuarioApp}?`)) return;
    const prev = u.activo;
    u.activo = nuevo;
    this.svc.cambiarEstadoUsuario(u.idUsuario, nuevo).subscribe({
      next: () => this.cdr.detectChanges(),
      error: (err: any) => { u.activo = prev; console.error(err); alert('No se pudo cambiar el estado.'); this.cdr.detectChanges(); }
    });
  }

  // --- Modal edicion de roles ---------------------------------

  openRolesAutoridadModal(a: AutoridadConRolesDTO): void {
    this.rolesModalTipo   = 'autoridad';
    this.selectedAutoridad = a;
    this.selectedUsuario   = null;
    this.selectedRolIds    = (a.rolesApp || []).map(r => r.idRolApp);
    this.showRolesModal    = true;
    this.cdr.detectChanges();
  }

  openRolesUsuarioModal(u: UsuarioConRolesDTO): void {
    this.rolesModalTipo  = 'usuario';
    this.selectedUsuario  = u;
    this.selectedAutoridad = null;
    this.selectedRolIds   = (u.rolesApp || []).map(r => r.idRolApp);
    this.showRolesModal   = true;
    this.cdr.detectChanges();
  }

  closeRolesModal(): void {
    this.showRolesModal    = false;
    this.selectedAutoridad = null;
    this.selectedUsuario   = null;
    this.cdr.detectChanges();
  }

  toggleRolApp(id: number, ev: any): void {
    if (ev?.target?.checked) { if (!this.selectedRolIds.includes(id)) this.selectedRolIds.push(id); }
    else { this.selectedRolIds = this.selectedRolIds.filter(x => x !== id); }
  }
  isRolSelected(id: number): boolean { return this.selectedRolIds.includes(id); }

  saveRoles(): void {
    this.isSaving = true;
    const ids = [...this.selectedRolIds];

    if (this.rolesModalTipo === 'autoridad' && this.selectedAutoridad) {
      this.svc.actualizarRolesAutoridad(this.selectedAutoridad.idAutoridad, ids).subscribe({
        next: updated => {
          const idx = this.autoridades.findIndex(a => a.idAutoridad === updated.idAutoridad);
          if (idx >= 0) this.autoridades[idx] = updated;
          this.applyFiltersAutoridades();
          this.isSaving = false; this.closeRolesModal(); alert('Roles actualizados.'); this.cdr.detectChanges();
        },
        error: (err: any) => { this.isSaving = false; console.error(err); alert('Error actualizando roles.'); this.cdr.detectChanges(); }
      });
    } else if (this.rolesModalTipo === 'usuario' && this.selectedUsuario) {
      this.svc.actualizarRolesUsuario(this.selectedUsuario.idUsuario, ids).subscribe({
        next: updated => {
          const idx = this.usuarios.findIndex(u => u.idUsuario === updated.idUsuario);
          if (idx >= 0) this.usuarios[idx] = updated;
          this.applyFiltersUsuarios();
          this.isSaving = false; this.closeRolesModal(); alert('Roles actualizados.'); this.cdr.detectChanges();
        },
        error: (err: any) => { this.isSaving = false; console.error(err); alert('Error actualizando roles.'); this.cdr.detectChanges(); }
      });
    }
  }

  // --- Modal creacion -----------------------------------------

  openCreate(): void {
    this.createModalTipo = this.activeTab === 'autoridades' ? 'autoridad' : 'usuario';
    this.formAutoridad   = { nombres: '', apellidos: '', correo: '', fechaNacimiento: '',
      idInstitucion: this.instituciones.length === 1 ? this.instituciones[0].idInstitucion : null };
    this.formUsuario     = { nombres: '', apellidos: '', correo: '' };
    this.createRolIds    = [];
    this.isCreating      = false;
    this.showCreateModal = true;
    this.cdr.detectChanges();
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.isCreating      = false;
    this.cdr.detectChanges();
  }

  toggleCreateRol(id: number, ev: any): void {
    if (ev?.target?.checked) { if (!this.createRolIds.includes(id)) this.createRolIds.push(id); }
    else { this.createRolIds = this.createRolIds.filter(x => x !== id); }
    this.cdr.detectChanges();
  }
  isCreateRolSelected(id: number): boolean { return this.createRolIds.includes(id); }

  saveCreate(): void {
    this.createModalTipo === 'autoridad' ? this._crearAutoridad() : this._crearUsuario();
  }

  private _crearAutoridad(): void {
    const { nombres, apellidos, correo, fechaNacimiento, idInstitucion } = this.formAutoridad;
    if (!nombres.trim())   { alert('Los nombres son obligatorios.');    return; }
    if (!apellidos.trim()) { alert('Los apellidos son obligatorios.');  return; }
    if (!correo.trim())    { alert('El correo es obligatorio.');        return; }
    if (!idInstitucion)    { alert('Selecciona una institucion.');      return; }
    if (!this.createRolIds.length) { alert('Selecciona al menos un rol.'); return; }

    this.isCreating = true;

    const rolesNombres = this.rolesAppDisponibles
      .filter(r => this.createRolIds.includes(r.idRolApp))
      .map(r => r.nombre);

    const payload: AutoridadCreatePayload = {
      nombres: nombres.trim(),
      apellidos: apellidos.trim(),
      correo: correo.trim(),
      fechaNacimiento: fechaNacimiento || null,
      idInstitucion,
      rolesApp: rolesNombres,
      idsRolAutoridad: []
    };

    this.svc.crearAutoridad(payload).subscribe({
      next: () => { this.loadAutoridades(); this.closeCreateModal(); alert('Autoridad creada. Se enviaron las credenciales por correo.'); this.cdr.detectChanges(); },
      error: (err: any) => {
        this.isCreating = false;
        const msg = err.error?.mensaje || err.error?.message || err.message || 'Error desconocido';
        console.error(err); alert('Error al crear la autoridad: ' + msg); this.cdr.detectChanges();
      }
    });
  }

  private _crearUsuario(): void {
    const { nombres, apellidos, correo } = this.formUsuario;
    if (!nombres.trim())   { alert('Los nombres son obligatorios.');   return; }
    if (!apellidos.trim()) { alert('Los apellidos son obligatorios.'); return; }
    if (!correo.trim())    { alert('El correo es obligatorio.');       return; }
    if (!this.createRolIds.length) { alert('Selecciona al menos un rol.'); return; }

    this.isCreating = true;

    const rolesNombres = this.rolesAppDisponibles
      .filter(r => this.createRolIds.includes(r.idRolApp))
      .map(r => r.nombre);

    const payload: UsuarioCreatePayload = {
      correo:    correo.trim(),
      nombres:   nombres.trim(),
      apellidos: apellidos.trim(),
      rolesApp:  rolesNombres
    };

    // POST /api/admin/usuarios → UsuarioAdminController.crearUsuario()
    // → AutoridadAcademicaService.registrarUsuario() → sp_registrar_usuario_simple
    this.svc.crearUsuario(payload).subscribe({
      next: () => { this.loadUsuarios(); this.closeCreateModal(); alert('Usuario creado. Se enviaron las credenciales por correo.'); this.cdr.detectChanges(); },
      error: (err: any) => {
        this.isCreating = false;
        const msg = err.error?.mensaje || err.error?.message || err.message || 'Error desconocido';
        console.error(err); alert('Error al crear el usuario: ' + msg); this.cdr.detectChanges();
      }
    });
  }

  // --- Helper paginacion --------------------------------------

  getPageNums(total: number): number[] {
    const arr: number[] = [];
    for (let i = 1; i <= total; i++) arr.push(i);
    return arr;
  }
}
