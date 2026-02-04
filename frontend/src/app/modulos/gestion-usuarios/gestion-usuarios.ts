// gestion-usuarios.component.ts

import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';


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
    private fb: FormBuilder
    // private usuarioService: UsuarioService,
    // private notificationService: NotificationService
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
      },
      {
        idUsuario: 3,
        idAutoridad: 3,
        usuarioApp: 'cgomez',
        nombres: 'Carlos Alberto',
        apellidos: 'Gómez Torres',
        correo: 'cgomez@uteq.edu.ec',
        activo: true,
        rolesAutoridad: ['VICERRECTORADO_ACADEMICO', 'CONSEJO_ACADEMICO'],
        rolesUsuario: ['ROLE_AUTORIDAD', 'ROLE_REPORT_VIEWER']
      },
      {
        idUsuario: 4,
        idAutoridad: 4,
        usuarioApp: 'lmartinez',
        nombres: 'Laura Patricia',
        apellidos: 'Martínez Vega',
        correo: 'lmartinez@uteq.edu.ec',
        activo: true,
        rolesAutoridad: ['DECANATO_FACULTAD', 'CONSEJO_ACADEMICO'],
        facultad: { id: 2, nombre: 'Ciencias Empresariales' },
        rolesUsuario: ['ROLE_AUTORIDAD', 'ROLE_EVALUATOR', 'ROLE_REPORT_VIEWER']
      },
      {
        idUsuario: 5,
        idAutoridad: 5,
        usuarioApp: 'rsanchez',
        nombres: 'Roberto José',
        apellidos: 'Sánchez Cruz',
        correo: 'rsanchez@uteq.edu.ec',
        activo: false,
        rolesAutoridad: ['COORDINADOR_CARRERA'],
        carrera: { id: 3, nombre: 'Administración de Empresas' },
        facultad: { id: 2, nombre: 'Ciencias Empresariales' },
        rolesUsuario: ['ROLE_AUTORIDAD', 'ROLE_ENTREVISTADOR']
      }
    ];

    this.usuariosFiltrados = [...this.usuarios];
    this.updateStats();
    this.calculatePagination();
  }

  loadCatalogs(): void {
    // Datos de ejemplo - Reemplazar con servicio real
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
      // Filtro de búsqueda
      const searchMatch = !this.searchTerm ||
        usuario.nombres.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        usuario.apellidos.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        usuario.correo.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        usuario.usuarioApp.toLowerCase().includes(this.searchTerm.toLowerCase());

      // Filtro de rol
      const roleMatch = !this.filterRole ||
        usuario.rolesAutoridad.includes(this.filterRole);

      // Filtro de estado
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
        pages.push(-1); // Representa "..."
      }

      const start = Math.max(2, this.currentPage - 1);
      const end = Math.min(this.totalPages - 1, this.currentPage + 1);

      for (let i = start; i <= end; i++) {
        pages.push(i);
      }

      if (this.currentPage < this.totalPages - 2) {
        pages.push(-1); // Representa "..."
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
      // Aquí iría la llamada al servicio
      // this.usuarioService.toggleStatus(usuario.idUsuario, newStatus).subscribe(...)

      usuario.activo = newStatus;
      this.updateStats();

      // this.notificationService.success(`Usuario ${action} exitosamente`);
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

    // Actualizar flags condicionales
    this.updateConditionalFields();

    // Actualizar preview de roles
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

    // Actualizar validadores
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

    if (selectedRoles.length === 0) {
      return;
    }

    // ROLE_AUTORIDAD - siempre se asigna si hay al menos un rol
    this.previewRoles.push({
      nombre: 'ROLE_AUTORIDAD',
      descripcion: 'Rol base para todas las autoridades'
    });

    // ROLE_EVALUATOR - si es Decano
    if (selectedRoles.includes('DECANATO_FACULTAD')) {
      this.previewRoles.push({
        nombre: 'ROLE_EVALUATOR',
        descripcion: 'Por rol Decanato de Facultad'
      });
    }

    // ROLE_REPORT_VIEWER - si es Vicerrectorado o Consejo
    if (selectedRoles.includes('VICERRECTORADO_ACADEMICO') || selectedRoles.includes('CONSEJO_ACADEMICO')) {
      this.previewRoles.push({
        nombre: 'ROLE_REPORT_VIEWER',
        descripcion: 'Por rol Vicerrectorado o Consejo Académico'
      });
    }

    // ROLE_ENTREVISTADOR - si es Coordinador
    if (selectedRoles.includes('COORDINADOR_CARRERA')) {
      this.previewRoles.push({
        nombre: 'ROLE_ENTREVISTADOR',
        descripcion: 'Por rol Coordinador de Carrera'
      });
    }
  }

  saveAutoridad(): void {
    if (this.createForm.invalid) {
      Object.keys(this.createForm.controls).forEach(key => {
        this.createForm.get(key)?.markAsTouched();
      });
      return;
    }

    // Obtener roles seleccionados
    const checkboxes = document.querySelectorAll<HTMLInputElement>('.checkbox-grid input[type="checkbox"]:checked');
    const rolesAutoridad: string[] = [];
    checkboxes.forEach(cb => rolesAutoridad.push(cb.value));

    if (rolesAutoridad.length === 0) {
      alert('Debe seleccionar al menos un rol de autoridad');
      return;
    }

    this.isSaving = true;

    const formData = {
      ...this.createForm.value,
      rolesAutoridad
    };

    console.log('Datos a enviar:', formData);

    // Simular llamada al backend
    setTimeout(() => {
      this.isSaving = false;
      this.closeCreateModal();
      alert('✅ Autoridad creada exitosamente\n\n📧 Se ha enviado un correo con las credenciales');

      // Recargar datos
      this.loadData();
    }, 1500);

    // En producción:
    // this.usuarioService.createAutoridad(formData).subscribe({
    //   next: (response) => {
    //     this.isSaving = false;
    //     this.closeCreateModal();
    //     this.notificationService.success('Autoridad creada exitosamente');
    //     this.loadData();
    //   },
    //   error: (error) => {
    //     this.isSaving = false;
    //     this.notificationService.error('Error al crear autoridad');
    //   }
    // });
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
      fechaNacimiento: '1980-05-15', // Obtener del backend
      idFacultad: usuario.facultad?.id || '',
      idCarrera: usuario.carrera?.id || ''
    });

    this.editNeedsFacultad = this.editRolesAutoridad.includes('DECANATO_FACULTAD');
    this.editNeedsCarrera = this.editRolesAutoridad.includes('COORDINADOR_CARRERA');

    // Cargar roles de usuario
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
    // Roles automáticos según roles de autoridad
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

    // Separar roles automáticos y adicionales
    const automaticRoles = ['ROLE_AUTORIDAD'];

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

    this.rolesAutomaticos = automaticRoles;
    this.rolesAdicionales = (usuario.rolesUsuario || []).filter(r => !automaticRoles.includes(r));

    // Roles disponibles para agregar
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

    console.log('Agregar rol:', this.newRoleToAdd, 'a usuario:', this.selectedUsuario.idUsuario);

    // Llamada al servicio
    this.rolesAdicionales.push(this.newRoleToAdd);
    this.availableRolesToAdd = this.availableRolesToAdd.filter(r => r !== this.newRoleToAdd);
    this.newRoleToAdd = '';

    alert('Rol agregado exitosamente');
  }

  removeRole(rol: string): void {
    if (confirm(`¿Está seguro que desea remover el rol ${rol}?`)) {
      console.log('Remover rol:', rol);

      this.rolesAdicionales = this.rolesAdicionales.filter(r => r !== rol);
      this.availableRolesToAdd.push(rol);

      alert('Rol removido exitosamente');
    }
  }
}
