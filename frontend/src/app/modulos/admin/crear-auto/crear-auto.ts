import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AutoridadAcademicaService } from '../../../services/autoridad-academica.service';
import { RolesAppService, RolAppConRolesBdDTO } from '../../../services/roles-app.service';
import { FacultadService } from '../../../services/facultad.service';
import { ToastService } from '../../../services/toast.service';

interface UsuarioDisponible {
  idUsuario: number;
  usuarioApp: string;
  correo: string;
}

@Component({
  selector: 'app-crear-evaluador',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './crear-auto.html',
  styleUrls: ['./crear-auto.scss']
})
export class CrearEvaluadorComponent implements OnInit {

  modoCreacion: 'nuevo' | 'existente' = 'nuevo';

  formNuevo = {
    nombres: '',
    apellidos: '',
    correo: '',
    fechaNacimiento: '',
    idInstitucion: null as number | null,
    idFacultad: null as number | null,
    rolesApp: [] as string[]
  };

  formExistente = {
    idUsuario: null as number | null,
    nombres: '',
    apellidos: '',
    correo: '',
    fechaNacimiento: '',
    idInstitucion: null as number | null,
    idFacultad: null as number | null,
    rolesApp: [] as string[]
  };

  instituciones: any[] = [];
  facultades: any[] = [];
  rolesDisponibles: RolAppConRolesBdDTO[] = [];
  usuariosDisponibles: UsuarioDisponible[] = [];

  isLoading = false;

  private readonly api = 'http://localhost:8080/api';

  constructor(
    private router: Router,
    private http: HttpClient,
    private autoridadService: AutoridadAcademicaService,
    private rolesAppService: RolesAppService,
    private facultadService: FacultadService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.autoridadService.listarInstituciones().subscribe({
      next: (d) => this.instituciones = d,
      error: () => this.toast.error('Error', 'No se pudieron cargar las instituciones')
    });
    this.facultadService.listar().subscribe({
      next: (d) => this.facultades = d.filter((f: any) => f.estado),
      error: () => this.toast.error('Error', 'No se pudieron cargar las facultades')
    });
    this.rolesAppService.listarParaAutoridad().subscribe({
      next: (d) => this.rolesDisponibles = d.filter(r => r.activo),
      error: () => this.toast.error('Error', 'No se pudieron cargar los roles')
    });
    this.http.get<UsuarioDisponible[]>(`${this.api}/autoridades-academicas/usuarios-disponibles`).subscribe({
      next: (d) => this.usuariosDisponibles = d,
      error: () => this.toast.error('Error', 'No se pudieron cargar los usuarios disponibles')
    });
  }

  toggleRolNuevo(nombre: string): void {
    const idx = this.formNuevo.rolesApp.indexOf(nombre);
    if (idx >= 0) this.formNuevo.rolesApp.splice(idx, 1);
    else this.formNuevo.rolesApp.push(nombre);
  }

  isRolSeleccionadoNuevo(nombre: string): boolean {
    return this.formNuevo.rolesApp.includes(nombre);
  }

  toggleRolExistente(nombre: string): void {
    const idx = this.formExistente.rolesApp.indexOf(nombre);
    if (idx >= 0) this.formExistente.rolesApp.splice(idx, 1);
    else this.formExistente.rolesApp.push(nombre);
  }

  isRolSeleccionadoExistente(nombre: string): boolean {
    return this.formExistente.rolesApp.includes(nombre);
  }

  onUsuarioSeleccionado(): void {
    const u = this.usuariosDisponibles.find(x => x.idUsuario === Number(this.formExistente.idUsuario));
    if (u) this.formExistente.correo = u.correo;
  }

  guardar(): void {
    if (this.modoCreacion === 'nuevo') {
      this.guardarNuevo();
    } else {
      this.guardarExistente();
    }
  }

  private guardarNuevo(): void {
    const f = this.formNuevo;
    if (!f.nombres || !f.apellidos || !f.correo || !f.fechaNacimiento || !f.idInstitucion) {
      this.toast.warning('Campos incompletos', 'Complete todos los campos obligatorios.');
      return;
    }
    if (f.rolesApp.length === 0) {
      this.toast.warning('Sin rol', 'Debe seleccionar al menos un rol.');
      return;
    }

    this.isLoading = true;
    const loadingId = this.toast.loading('Guardando...', 'Creando autoridad académica');

    this.autoridadService.registrarAutoridad({
      nombres: f.nombres,
      apellidos: f.apellidos,
      correo: f.correo,
      fechaNacimiento: f.fechaNacimiento,
      idInstitucion: f.idInstitucion!,
      idFacultad: f.idFacultad,
      rolesApp: f.rolesApp,
      idsRolAutoridad: []
    }).subscribe({
      next: (res: any) => {
        this.isLoading = false;
        this.toast.remove(loadingId);
        this.toast.success('Autoridad creada', `Usuario: ${res.usuarioApp}. Credenciales enviadas al correo.`);
        setTimeout(() => this.router.navigate(['/admin']), 2500);
      },
      error: (err: any) => {
        this.isLoading = false;
        this.toast.remove(loadingId);
        this.toast.error('Error al crear', err?.error?.message || 'No se pudo crear la autoridad.');
      }
    });
  }

  private guardarExistente(): void {
    const f = this.formExistente;
    if (!f.idUsuario || !f.nombres || !f.apellidos || !f.fechaNacimiento || !f.idInstitucion) {
      this.toast.warning('Campos incompletos', 'Complete todos los campos obligatorios.');
      return;
    }
    if (f.rolesApp.length === 0) {
      this.toast.warning('Sin rol', 'Debe seleccionar al menos un rol.');
      return;
    }

    this.isLoading = true;
    const loadingId = this.toast.loading('Vinculando...', 'Asociando usuario como autoridad académica');

    this.http.post<any>(`${this.api}/autoridades-academicas/desde-usuario`, {
      idUsuario: Number(f.idUsuario),
      nombres: f.nombres,
      apellidos: f.apellidos,
      correo: f.correo,
      fechaNacimiento: f.fechaNacimiento,
      idInstitucion: f.idInstitucion,
      idFacultad: f.idFacultad,
      rolesApp: f.rolesApp
    }).subscribe({
      next: (res: any) => {
        this.isLoading = false;
        this.toast.remove(loadingId);
        this.toast.success('Autoridad vinculada', `Usuario: ${res.usuarioApp}.`);
        setTimeout(() => this.router.navigate(['/admin']), 2500);
      },
      error: (err: any) => {
        this.isLoading = false;
        this.toast.remove(loadingId);
        this.toast.error('Error al vincular', err?.error?.message || 'No se pudo vincular la autoridad.');
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/admin']);
  }
}
