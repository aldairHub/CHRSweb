import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';

import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

import {
  SolicitudDocenteService,
  SolicitudDocenteRequest,
  SolicitudConCredenciales,
  Carrera,
  Materia,
  AreaConocimiento
} from '../../../services/SolicitudDocente.service';

@Component({
  selector: 'app-solicitar-docente',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HttpClientModule,
    NavbarComponent,
    FooterComponent
  ],
  templateUrl: './solicitar-docente.html',
  styleUrls: ['./solicitar-docente.scss'],
  providers: [SolicitudDocenteService]
})
export class SolicitarDocenteComponent implements OnInit {

  solicitud = this.crearSolicitudVacia();

  credenciales = {
    usuarioBd: '',
    claveBd: ''
  };

  carreras: Carrera[] = [];
  materias: Materia[] = [];
  areas: AreaConocimiento[] = [];

  loadingMaterias = false;
  guardando = false;

  // ⭐ TOAST
  showToast = false;
  toastMessage = '';
  toastTitle = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  nivelesAcademicos = [
    { value: 'Pregrado', label: 'Pregrado' },
    { value: 'Especialización', label: 'Especialización' },
    { value: 'Maestría', label: 'Maestría' },
    { value: 'Doctorado', label: 'Doctorado' },
    { value: 'Postdoctorado', label: 'Postdoctorado' }
  ];

  constructor(
    private router: Router,
    private solicitudService: SolicitudDocenteService
  ) {}

  ngOnInit(): void {
    this.cargarCredenciales();
    this.cargarCatalogos();
  }

  crearSolicitudVacia() {
    return {
      id_carrera: null,
      id_area: null,
      id_materia: null,
      nivel_academico: '',
      cantidad_docentes: 1,
      experiencia_profesional_min: 0,
      experiencia_docente_min: 0,
      justificacion: '',
      observaciones: ''
    };
  }

  cargarCredenciales(): void {
    const usuario = localStorage.getItem('usuario');
    const token = localStorage.getItem('token');

    if (!usuario || !token) {
      this.mostrarToast('error', 'Sesión expirada', 'Por favor, inicie sesión nuevamente.');
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }

    this.credenciales.usuarioBd = usuario;
    this.credenciales.claveBd = 'dummy';
  }

  cargarCatalogos(): void {
    this.solicitudService.obtenerCarreras().subscribe({
      next: c => this.carreras = c,
      error: () => this.mostrarToast('error','Error','No se pudieron cargar las carreras.')
    });

    this.solicitudService.obtenerAreasConocimiento().subscribe({
      next: a => this.areas = a
    });
  }

  onCarreraChange(): void {

    this.solicitud.id_materia = null;

    if (!this.solicitud.id_carrera) {
      this.materias = [];
      return;
    }

    this.loadingMaterias = true;

    this.solicitudService
      .obtenerMateriasPorCarrera(Number(this.solicitud.id_carrera))
      .subscribe({
        next: m => {
          console.log("MATERIAS RECIBIDAS 👉", m);
          this.materias = m;
          this.loadingMaterias = false;
        },
        error: () => {
          this.mostrarToast('error','Error','No se pudieron cargar las materias.');
          this.loadingMaterias = false;
        }
      });
  }

  guardarSolicitud(form: any): void {

    if (!this.validarFormulario()) return;

    const solicitudRequest: SolicitudDocenteRequest = {
      idCarrera: Number(this.solicitud.id_carrera),
      idMateria: Number(this.solicitud.id_materia),
      idArea: Number(this.solicitud.id_area),
      cantidadDocentes: Number(this.solicitud.cantidad_docentes),
      nivelAcademico: this.solicitud.nivel_academico,
      experienciaProfesionalMin: Number(this.solicitud.experiencia_profesional_min),
      experienciaDocenteMin: Number(this.solicitud.experiencia_docente_min),
      justificacion: this.solicitud.justificacion,
      observaciones: this.solicitud.observaciones || undefined
    };

    const request: SolicitudConCredenciales = {
      usuarioBd: this.credenciales.usuarioBd,
      claveBd: this.credenciales.claveBd,
      solicitud: solicitudRequest
    };
    this.guardando = true;
    this.solicitudService.crearSolicitudConCredenciales(request)
      .subscribe({
        next: r => {

          const mensaje =
            `Solicitud #${r.idSolicitud} creada exitosamente para ${r.nombreCarrera} - ${r.nombreMateria}`;

          this.mostrarToast('success', '¡Solicitud Creada!', mensaje);

          setTimeout(() => {
            form.resetForm();
            this.solicitud = this.crearSolicitudVacia();
            this.materias = [];
          }, 400);
        },
        error: e => {

          let mensaje = 'No se pudo guardar la solicitud.';

          if (e.error?.mensaje) mensaje = e.error.mensaje;
          else if (e.status === 400) mensaje = 'Datos inválidos.';
          else if (e.status === 401 || e.status === 403) {
            mensaje = 'Sesión inválida.';
            localStorage.clear();
            setTimeout(() => this.router.navigate(['/login']), 2000);
          }
          else if (e.status === 0) mensaje = 'Servidor no disponible.';

          this.mostrarToast('error', 'Error al Guardar', mensaje);
        }
      });
  }

  validarFormulario(): boolean {

    if (!this.solicitud.id_carrera ||
      !this.solicitud.id_materia ||
      !this.solicitud.id_area) {

      this.mostrarToast('error','Campos Incompletos','Complete los campos obligatorios.');
      return false;
    }

    if (!this.solicitud.nivel_academico) {
      this.mostrarToast('error','Nivel Académico','Seleccione nivel académico.');
      return false;
    }

    if (this.solicitud.cantidad_docentes < 1) {
      this.mostrarToast('error','Cantidad','Debe solicitar al menos 1 docente.');
      return false;
    }

    if (this.solicitud.experiencia_profesional_min < 0) {
      this.mostrarToast('error','Experiencia Profesional','No puede ser negativa.');
      return false;
    }

    if (this.solicitud.experiencia_docente_min < 0) {
      this.mostrarToast('error','Experiencia Docente','No puede ser negativa.');
      return false;
    }

    if (!this.solicitud.justificacion || this.solicitud.justificacion.trim().length < 20) {
      this.mostrarToast('error','Justificación','Mínimo 20 caracteres.');
      return false;
    }

    return true;
  }


  cancelar(): void {

    const tieneContenido =
      this.solicitud.id_carrera ||
      this.solicitud.justificacion ||
      this.solicitud.observaciones;

    if (tieneContenido) {
      if (confirm('Se perderán los cambios. ¿Continuar?')) {
        this.router.navigate(['/evaluador']);
      }
    } else {
      this.router.navigate(['/evaluador']);
    }
  }

  mostrarToast(tipo: 'success' | 'error', titulo: string, mensaje: string): void {

    this.toastType = tipo;
    this.toastTitle = titulo;
    this.toastMessage = mensaje;
    this.showToast = true;

    clearTimeout(this.toastTimer);

    this.toastTimer = setTimeout(() => {
      this.showToast = false;
    }, 1800);
  }

  cerrarToast(): void {
    this.showToast = false;
    clearTimeout(this.toastTimer);
  }

}
