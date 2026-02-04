// gestion-usuarios.component.ts

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';

// 1. IMPORTAMOS EL SERVICIO DE USUARIOS
import { UsuarioService } from '../../services/usuario.service';

// Interfaces
interface Usuario {
  idUsuario: number;
  idAutoridad: number;
  usuarioApp: string;
  nombres: string;
  apellidos: string;
  correo: string;
  activo: boolean;
  rolesAutoridad: string[];
  facultad?: { id: number; nombre: string };
  carrera?: { id: number; nombre: string };
  rolesUsuario?: string[];
}

interface Institucion {
  id: number;
  nombre: string;
}

interface Facultad {
  id: number;
  nombre: string;
}

interface Carrera {
  id: number;
  nombre: string;
  idFacultad: number;
}

interface RolPreview {
  nombre: string;
  descripcion: string;
}

interface RolUsuario {
  nombre: string;
  descripcion: string;
  automatico: boolean;
  activo: boolean;

}

interface RolAutoridad {
  id: string;
  nombre: string;
}

@Component({
  selector: 'app-gestion-usuarios',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './gestion-usuarios.html',
  styleUrls: ['./gestion-usuarios.scss']
})

export class GestionUsuariosComponent implements OnInit {

  // Datos de la tabla
  usuarios: Usuario[] = [];
  usuariosFiltrados: Usuario[] = [];

  // Estadísticas
  totalUsuarios = 0;
  usuariosActivos = 0;
  totalDecanos = 0;
  totalCoordinadores = 0;

  // Filtros
  searchTerm = '';
  filterRole = '';
  filterStatus = '';

  // Paginación
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;
  Math = Math;

  // Modales
  showCreateModal = false;
  showEditModal = false;
  showRolesModal = false;

  // Formularios
  createForm!: FormGroup;
  editForm!: FormGroup;

  // Loading states
  isSaving = false;
  isUpdating = false;

  // Usuario seleccionado
  selectedUsuario: Usuario | null = null;

  // Datos para selects
  instituciones: Institucion[] = [];
  facultades: Facultad[] = [];
  carreras: Carrera[] = [];
  rolesAutoridadDisponibles: RolAutoridad[] = [
    { id: 'DECANATO_FACULTAD', nombre: 'Decanato de la Facultad' },
    { id: 'COORDINADOR_CARRERA', nombre: 'Coordinador de Carrera' },
    { id: 'VICERRECTORADO_ACADEMICO', nombre: 'Miembro de Vicerrectorado' },
    { id: 'CONSEJO_ACADEMICO', nombre: 'Miembro del Consejo Académico' }
  ];

  // Preview de roles
  previewRoles: RolPreview[] = [];

  // Flags condicionales
  needsFacultad = false;
  needsCarrera = false;
  editNeedsFacultad = false;
  editNeedsCarrera = false;

  // Roles en edición
  editRolesAutoridad: string[] = [];
  editRolesUsuario: RolUsuario[] = [];

  // Modal de roles
  rolesAutomaticos: string[] = [];
  rolesAdicionales: string[] = [];
  availableRolesToAdd: string[] = [];
  newRoleToAdd = '';

  constructor(
    private fb: FormBuilder,
    // 2. INYECTAMOS EL SERVICIO
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.initForms();
    this.loadData();
    this.loadCatalogs();
  }

  // ==========================================
  // INICIALIZACIÓN
  // ==========================================

  initForms(): void {
    this.createForm = this.fb.group({
      nombres: ['', [Validators.required, Validators.minLength(2)]],
      apellidos: ['', [Validators.required, Validators.minLength(2)]],
      correo: ['', [Validators.required, Validators.email]],
      fechaNacimiento: ['', Validators.required],
      idInstitucion: ['', Validators.required],
      rolesAutoridad: this.fb.array([]),
      idFacultad: [''],
      idCarrera: ['']
    });

    this.editForm = this.fb.group({
      nombres: ['', [Validators.required]],
      apellidos: ['', [Validators.required]],
      correo: ['', [Validators.required, Validators.email]],
      fechaNacimiento: ['', Validators.required],
      idFacultad: [''],
      idCarrera: ['']
    });
  }

  loadData(): void {
    // Datos de ejemplo - Reemplazar con servicio real
    // OPCIONAL: Podrías llamar a this.usuarioService.listarTodos() aquí
    this.usuarios = [
      {
        idUsuario: 1,
        idAutoridad: 1,
        usuarioApp: 'jperez',
        nombres: 'Juan Carlos',
        apellidos: 'Pérez Morales',
        correo: 'jperez@uteq.edu.ec',
        activo: true,
        rolesAutoridad: ['DECANATO_FACULTAD'],
        facultad: { id: 1, nombre: 'Ciencias de la Computación' },
        rolesUsuario: ['ROLE_AUTORIDAD', 'ROLE_EVALUATOR']
      },
      // ... (Tus datos de ejemplo siguen aquí)
      {
        idUsuario: 2,
        idAutoridad: 2,
        usuarioApp: 'mrodriguez',
        nombres: 'María Elena',
        apellidos: 'Rodríguez Silva',
        correo: 'mrodriguez@uteq.edu.ec',
        activo: true,
        rolesAutoridad: ['COORDINADOR_CARRERA'],
        carrera: { id: 1, nombre: 'Ingeniería en Software' },
        facultad: { id: 1, nombre: 'Ciencias de la Computación' },
        rolesUsuario: ['ROLE_AUTORIDAD', 'ROLE_ENTREVISTADOR']
      }
    ];

    this.usuariosFiltrados = [...this.usuarios];
    this.updateStats();
    this.calculatePagination();
  }

  loadCatalogs(): void {
    this.instituciones = [
      { id: 1, nombre: 'Universidad Técnica Estatal de Quevedo (UTEQ)' },
      { id: 2, nombre: 'Campus Buena Fe' },
      { id: 3, nombre: 'Campus Valencia' }
    ];

    this.facultades = [
      { id: 1, nombre: 'Ciencias de la Computación y Diseño Digital' },
      { id: 2, nombre: 'Ciencias Empresariales' },
      { id: 3, nombre: 'Ciencias de la Ingeniería' },
      { id: 4, nombre: 'Ciencias Pecuarias' }
    ];

    this.carreras = [
      { id: 1, nombre: 'Ingeniería en Software', idFacultad: 1 },
      { id: 2, nombre: 'Diseño Gráfico', idFacultad: 1 },
      { id: 3, nombre: 'Administración de Empresas', idFacultad: 2 },
      { id: 4, nombre: 'Marketing Digital', idFacultad: 2 },
      { id: 5, nombre: 'Ingeniería Civil', idFacultad: 3 }
    ];
  }

  // ==========================================
  // ESTADÍSTICAS
  // ==========================================

  updateStats(): void {
    this.totalUsuarios = this.usuarios.length;
    this.usuariosActivos = this.usuarios.filter(u => u.activo).length;
    this.totalDecanos = this.usuarios.filter(u =>
      u.rolesAutoridad.includes('DECANATO_FACULTAD')
    ).length;
    this.totalCoordinadores = this.usuarios.filter(u =>
      u.rolesAutoridad.includes('COORDINADOR_CARRERA')
    ).length;
  }

  // ==========================================
  // FILTROS Y BÚSQUEDA
  // ==========================================

  applyFilters(): void {
    this.usuariosFiltrados = this.usuarios.filter(usuario => {
      const searchMatch = !this.searchTerm ||
        usuario.nombres.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        usuario.apellidos.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        usuario.correo.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        usuario.usuarioApp.toLowerCase().includes(this.searchTerm.toLowerCase());

      const roleMatch = !this.filterRole ||
        usuario.rolesAutoridad.includes(this.filterRole);

      const statusMatch = !this.filterStatus ||
        usuario.activo.toString() === this.filterStatus;

      return searchMatch && roleMatch && statusMatch;
    });

    this.currentPage = 1;
    this.calculatePagination();
  }

  // ==========================================
  // PAGINACIÓN
  // ==========================================

  calculatePagination(): void {
    this.totalPages = Math.ceil(this.usuariosFiltrados.length / this.pageSize);
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
      for (let i = 1; i <= this.totalPages; i++) {
        pages.push(i);
      }
    } else {
      pages.push(1);
      if (this.currentPage > 3) {
        pages.push(-1);
      }

      const start = Math.max(2, this.currentPage - 1);
      const end = Math.min(this.totalPages - 1, this.currentPage + 1);

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (this.currentPage < this.totalPages - 2) {
        pages.push(-1);
      }
      pages.push(this.totalPages);
    }

    return pages;
  }

  // ==========================================
  // UTILIDADES DE ROLES
  // ==========================================

  getRolClass(rol: string): string {
    const classMap: { [key: string]: string } = {
      'DECANATO_FACULTAD': 'decano',
      'COORDINADOR_CARRERA': 'coordinador',
      'VICERRECTORADO_ACADEMICO': 'vicerrectorado',
      'CONSEJO_ACADEMICO': 'consejo'
    };
    return classMap[rol] || '';
  }

  getRolLabel(rol: string): string {
    const labelMap: { [key: string]: string } = {
      'DECANATO_FACULTAD': 'Decano',
      'COORDINADOR_CARRERA': 'Coordinador',
      'VICERRECTORADO_ACADEMICO': 'Vicerrectorado',
      'CONSEJO_ACADEMICO': 'Consejo'
    };
    return labelMap[rol] || rol;
  }

  // ==========================================
  // TOGGLE STATUS
  // ==========================================

  toggleStatus(usuario: Usuario): void {
    const newStatus = !usuario.activo;
    const action = newStatus ? 'activar' : 'desactivar';

    if (confirm(`¿Está seguro que desea ${action} este usuario?\n\n${newStatus ? 'El usuario podrá acceder al sistema.' : 'El usuario NO podrá acceder al sistema.'}`)) {
      usuario.activo = newStatus;
      this.updateStats();
      alert(`Usuario ${action} exitosamente`);
    }
  }

  // ==========================================
  // MODAL CREAR
  // ==========================================

  openCreateModal(): void {
    this.createForm.reset();
    this.previewRoles = [];
    this.needsFacultad = false;
    this.needsCarrera = false;
    this.showCreateModal = true;
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
  }

  onRoleChange(event: any): void {
    const value = event.target.value;
    const checked = event.target.checked;
    this.updateConditionalFields();
    this.updateRolesPreview();
  }

  updateConditionalFields(): void {
    const checkboxes = document.querySelectorAll<HTMLInputElement>('input[type="checkbox"][value]');
    const selectedRoles: string[] = [];

    checkboxes.forEach(cb => {
      if (cb.checked) {
        selectedRoles.push(cb.value);
      }
    });

    this.needsFacultad = selectedRoles.includes('DECANATO_FACULTAD');
    this.needsCarrera = selectedRoles.includes('COORDINADOR_CARRERA');

    if (this.needsFacultad) {
      this.createForm.get('idFacultad')?.setValidators([Validators.required]);
    } else {
      this.createForm.get('idFacultad')?.clearValidators();
      this.createForm.get('idFacultad')?.setValue('');
    }

    if (this.needsCarrera) {
      this.createForm.get('idCarrera')?.setValidators([Validators.required]);
    } else {
      this.createForm.get('idCarrera')?.clearValidators();
      this.createForm.get('idCarrera')?.setValue('');
    }

    this.createForm.get('idFacultad')?.updateValueAndValidity();
    this.createForm.get('idCarrera')?.updateValueAndValidity();
  }

  updateRolesPreview(): void {
    const checkboxes = document.querySelectorAll<HTMLInputElement>('input[type="checkbox"][value]');
    const selectedRoles: string[] = [];

    checkboxes.forEach(cb => {
      if (cb.checked) {
        selectedRoles.push(cb.value);
      }
    });

    this.previewRoles = [];
    if (selectedRoles.length === 0) return;

    this.previewRoles.push({
      nombre: 'ROLE_AUTORIDAD',
      descripcion: 'Rol base para todas las autoridades'
    });

    if (selectedRoles.includes('DECANATO_FACULTAD')) {
      this.previewRoles.push({
        nombre: 'ROLE_EVALUATOR',
        descripcion: 'Por rol Decanato de Facultad'
      });
    }

    if (selectedRoles.includes('VICERRECTORADO_ACADEMICO') || selectedRoles.includes('CONSEJO_ACADEMICO')) {
      this.previewRoles.push({
        nombre: 'ROLE_REPORT_VIEWER',
        descripcion: 'Por rol Vicerrectorado o Consejo Académico'
      });
    }

    if (selectedRoles.includes('COORDINADOR_CARRERA')) {
      this.previewRoles.push({
        nombre: 'ROLE_ENTREVISTADOR',
        descripcion: 'Por rol Coordinador de Carrera'
      });
    }
  }

  // ==========================================
  // SAVE AUTORIDAD (MODIFICADO PARA BACKEND)
  // ==========================================

  saveAutoridad(): void {
    if (this.createForm.invalid) {
      Object.keys(this.createForm.controls).forEach(key => {
        this.createForm.get(key)?.markAsTouched();
      });
      return;
    }

    this.isSaving = true;

    // 1. Obtener datos del formulario
    const formValues = this.createForm.value;
    const correo = formValues.correo;
    const usuarioGenerado = correo.split('@')[0]; // Generar usuario desde el correo

    // 2. Preparar el JSON EXACTO para tu Backend
    const usuarioParaBackend = {
      usuarioApp: usuarioGenerado,
      claveApp: 'UtEq2026.',       // Contraseña por defecto
      correo: correo,
      usuarioBd: 'invitado_bd',    // Dato obligatorio por tu backend
      claveBd: 'clave_bd_123',     // Dato obligatorio por tu backend
      activo: true
    };

    console.log('Enviando al backend:', usuarioParaBackend);

    // 3. Llamada al servicio
    this.usuarioService.crear(usuarioParaBackend).subscribe({
      next: (res) => {
        this.isSaving = false;
        this.closeCreateModal();

        // Mensaje de éxito con los datos reales creados
        alert(`✅ Usuario creado exitosamente!\n\nID: ${res.idUsuario}\nUsuario: ${res.usuarioApp}\nCorreo: ${res.correo}`);

        // Opcional: Recargar la tabla si implementas listarTodos()
        // this.loadData();
      },
      error: (err) => {
        this.isSaving = false;
        console.error('Error del servidor:', err);
        const mensaje = err.error || err.message || 'Error desconocido';
        alert('❌ Error al crear usuario: ' + mensaje);
      }
    });
  }

  // ==========================================
  // MODAL EDITAR
  // ==========================================

  openEditModal(usuario: Usuario): void {
    this.selectedUsuario = usuario;
    this.editRolesAutoridad = [...usuario.rolesAutoridad];

    this.editForm.patchValue({
      nombres: usuario.nombres,
      apellidos: usuario.apellidos,
      correo: usuario.correo,
      fechaNacimiento: '1980-05-15',
      idFacultad: usuario.facultad?.id || '',
      idCarrera: usuario.carrera?.id || ''
    });

    this.editNeedsFacultad = this.editRolesAutoridad.includes('DECANATO_FACULTAD');
    this.editNeedsCarrera = this.editRolesAutoridad.includes('COORDINADOR_CARRERA');

    this.loadEditRolesUsuario(usuario);
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedUsuario = null;
  }

  onEditRoleChange(rolId: string, event: any): void {
    const checked = event.target.checked;

    if (checked) {
      if (!this.editRolesAutoridad.includes(rolId)) {
        this.editRolesAutoridad.push(rolId);
      }
    } else {
      this.editRolesAutoridad = this.editRolesAutoridad.filter(r => r !== rolId);
    }

    this.editNeedsFacultad = this.editRolesAutoridad.includes('DECANATO_FACULTAD');
    this.editNeedsCarrera = this.editRolesAutoridad.includes('COORDINADOR_CARRERA');
  }

  loadEditRolesUsuario(usuario: Usuario): void {
    const automaticRoles: string[] = ['ROLE_AUTORIDAD'];

    if (usuario.rolesAutoridad.includes('DECANATO_FACULTAD')) {
      automaticRoles.push('ROLE_EVALUATOR');
    }
    if (usuario.rolesAutoridad.includes('VICERRECTORADO_ACADEMICO') ||
      usuario.rolesAutoridad.includes('CONSEJO_ACADEMICO')) {
      automaticRoles.push('ROLE_REPORT_VIEWER');
    }
    if (usuario.rolesAutoridad.includes('COORDINADOR_CARRERA')) {
      automaticRoles.push('ROLE_ENTREVISTADOR');
    }

    this.editRolesUsuario = [
      ...automaticRoles.map(rol => ({
        nombre: rol,
        descripcion: this.getRolUsuarioDescription(rol),
        automatico: true,
        activo: true
      })),
      {
        nombre: 'ROLE_CONTENT_MANAGER',
        descripcion: 'Gestión de contenidos',
        automatico: false,
        activo: false
      },
      {
        nombre: 'ROLE_SYSTEM_MONITOR',
        descripcion: 'Monitoreo del sistema',
        automatico: false,
        activo: false
      }
    ];
  }

  getRolUsuarioDescription(rol: string): string {
    const descriptions: { [key: string]: string } = {
      'ROLE_AUTORIDAD': 'Rol base para autoridades',
      'ROLE_EVALUATOR': 'Por rol Decanato',
      'ROLE_REPORT_VIEWER': 'Por rol Vicerrectorado/Consejo',
      'ROLE_ENTREVISTADOR': 'Por rol Coordinador'
    };
    return descriptions[rol] || 'Rol adicional';
  }

  toggleRolUsuario(rol: RolUsuario): void {
    rol.activo = !rol.activo;
  }

  updateAutoridad(): void {
    if (this.editForm.invalid) {
      return;
    }

    this.isUpdating = true;

    const updateData = {
      ...this.editForm.value,
      rolesAutoridad: this.editRolesAutoridad,
      rolesUsuario: this.editRolesUsuario.filter(r => r.activo && !r.automatico).map(r => r.nombre)
    };

    console.log('Datos de actualización:', updateData);

    setTimeout(() => {
      this.isUpdating = false;
      this.closeEditModal();
      alert('Autoridad actualizada exitosamente');
      this.loadData();
    }, 1500);
  }

  // ==========================================
  // MODAL ROLES
  // ==========================================

  openRolesModal(usuario: Usuario): void {
    this.selectedUsuario = usuario;

    const automaticRoles = ['ROLE_AUTORIDAD'];
    if (usuario.rolesAutoridad.includes('DECANATO_FACULTAD')) automaticRoles.push('ROLE_EVALUATOR');
    if (usuario.rolesAutoridad.includes('VICERRECTORADO_ACADEMICO') || usuario.rolesAutoridad.includes('CONSEJO_ACADEMICO')) automaticRoles.push('ROLE_REPORT_VIEWER');
    if (usuario.rolesAutoridad.includes('COORDINADOR_CARRERA')) automaticRoles.push('ROLE_ENTREVISTADOR');

    this.rolesAutomaticos = automaticRoles;
    this.rolesAdicionales = (usuario.rolesUsuario || []).filter(r => !automaticRoles.includes(r));

    this.availableRolesToAdd = [
      'ROLE_CONTENT_MANAGER',
      'ROLE_SYSTEM_MONITOR',
      'ROLE_DATA_ANALYST',
      'ROLE_AUDIT_VIEWER'
    ].filter(r => ![...automaticRoles, ...this.rolesAdicionales].includes(r));

    this.newRoleToAdd = '';
    this.showRolesModal = true;
  }

  closeRolesModal(): void {
    this.showRolesModal = false;
    this.selectedUsuario = null;
  }

  addRole(): void {
    if (!this.newRoleToAdd || !this.selectedUsuario) {
      return;
    }
    this.rolesAdicionales.push(this.newRoleToAdd);
    this.availableRolesToAdd = this.availableRolesToAdd.filter(r => r !== this.newRoleToAdd);
    this.newRoleToAdd = '';
    alert('Rol agregado exitosamente');
  }

  removeRole(rol: string): void {
    if (confirm(`¿Está seguro que desea remover el rol ${rol}?`)) {
      this.rolesAdicionales = this.rolesAdicionales.filter(r => r !== rol);
      this.availableRolesToAdd.push(rol);
      alert('Rol removido exitosamente');
    }
  }
}
