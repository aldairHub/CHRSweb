import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClientModule } from '@angular/common/http';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer';

import { NivelAcademicoService, NivelAcademico } from '../../../services/nivel-academico.service';
import {
  SolicitudDocenteService,
  SolicitudDocenteRequest,
  SolicitudConCredenciales,
  Carrera,
  Materia,
  AreaConocimiento
} from '../../../services/solicitud-docente.service';

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
export class SolicitarDocenteComponent implements OnInit, OnDestroy {

  solicitud = this.crearSolicitudVacia();

  credenciales = { usuarioBd: '', claveBd: '' };

  carreras: Carrera[]          = [];
  materias: Materia[]          = [];
  areas:    AreaConocimiento[] = [];
  nivelesAcademicos: NivelAcademico[] = [];

  loadingCarreras  = false;
  loadingMaterias  = false;
  guardando        = false;

  showToast    = false;
  toastMessage = '';
  toastTitle   = '';
  toastType: 'success' | 'error' = 'success';
  private toastTimer: any;

  private destroy$ = new Subject<void>();

  constructor(
    private router: Router,
    private solicitudService: SolicitudDocenteService,
    private nivelSvc: NivelAcademicoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargarCredenciales();
    this.cargarNiveles();
    this.cargarCatalogos();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ── Niveles académicos ────────────────────────────────────────────────
  cargarNiveles(): void {
    this.nivelSvc.listarActivos()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: data => { this.nivelesAcademicos = data; this.cdr.detectChanges(); },
        error: () => {}
      });
  }

  // ── Credenciales del usuario logueado ─────────────────────────────────
  cargarCredenciales(): void {
    const usuario = localStorage.getItem('usuario');
    const token   = localStorage.getItem('token');

    if (!usuario || !token) {
      this.mostrarToast('error', 'Sesión expirada', 'Por favor, inicie sesión nuevamente.');
      setTimeout(() => this.router.navigate(['/login']), 2000);
      return;
    }

    this.credenciales.usuarioBd = usuario;
    this.credenciales.claveBd   = 'dummy';
  }

  // ── Carga inicial de catálogos ────────────────────────────────────────
  // Las carreras se filtran automáticamente por la facultad del evaluador:
  // 1. Se consulta GET /api/solicitudes-docente/mi-facultad?usuarioApp=...
  // 2. Con el idFacultad retornado, se llama a GET /api/carreras/por-facultad/{id}
  // El evaluador nunca ve un dropdown de facultad — solo sus carreras.
  cargarCatalogos(): void {
    const usuarioApp = localStorage.getItem('usuario');
    if (!usuarioApp) return;

    // Áreas (no dependen de la facultad)
    this.solicitudService.obtenerAreasConocimiento()
      .pipe(takeUntil(this.destroy$))
      .subscribe({ next: a => this.areas = a });

    // Carreras filtradas por la facultad del evaluador
    this.loadingCarreras = true;

    this.solicitudService.obtenerMiFacultad(usuarioApp)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ({ idFacultad }) => {
          this.solicitudService.obtenerCarrerasPorFacultad(idFacultad)
            .pipe(takeUntil(this.destroy$))
            .subscribe({
              next: c => {
                this.carreras        = c;
                this.loadingCarreras = false;
                this.cdr.detectChanges();
              },
              error: () => {
                this.loadingCarreras = false;
                this.mostrarToast('error', 'Error', 'No se pudieron cargar las carreras.');
              }
            });
        },
        error: () => {
          this.loadingCarreras = false;
          this.mostrarToast('error', 'Error', 'No se pudo determinar tu facultad asignada.');
        }
      });
  }

  // ── Cuando cambia la carrera → cargar materias ────────────────────────
  onCarreraChange(): void {
    this.solicitud.id_materia = null;
    this.materias             = [];

    if (!this.solicitud.id_carrera) return;

    this.loadingMaterias = true;

    this.solicitudService.obtenerMateriasPorCarrera(Number(this.solicitud.id_carrera))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: m => {
          this.materias        = m;
          this.loadingMaterias = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.loadingMaterias = false;
          this.mostrarToast('error', 'Error', 'No se pudieron cargar las materias.');
        }
      });
  }

  // ── Guardar solicitud ─────────────────────────────────────────────────
  guardarSolicitud(form: any): void {
    if (!this.validarFormulario()) return;

    const solicitudRequest: SolicitudDocenteRequest = {
      idCarrera:                 Number(this.solicitud.id_carrera),
      idMateria:                 Number(this.solicitud.id_materia),
      idArea:                    Number(this.solicitud.id_area),
      cantidadDocentes:          Number(this.solicitud.cantidad_docentes),
      nivelAcademico:            this.solicitud.nivel_academico,
      experienciaProfesionalMin: Number(this.solicitud.experiencia_profesional_min),
      experienciaDocenteMin:     Number(this.solicitud.experiencia_docente_min),
      justificacion:             this.solicitud.justificacion,
      // observaciones:             this.solicitud.observaciones || undefined
    };

    const request: SolicitudConCredenciales = {
      usuarioBd: this.credenciales.usuarioBd,
      claveBd:   this.credenciales.claveBd,
      solicitud: solicitudRequest
    };

    this.guardando = true;

    this.solicitudService.crearSolicitudConCredenciales(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: r => {
          this.guardando = false;
          this.mostrarToast(
            'success',
            '¡Solicitud Creada!',
            `Solicitud #${r.idSolicitud} creada para ${r.nombreCarrera} - ${r.nombreMateria}`
          );
          setTimeout(() => {
            form.resetForm();
            this.solicitud = this.crearSolicitudVacia();
            this.materias  = [];
          }, 400);
        },
        error: e => {
          this.guardando = false;
          let mensaje = 'No se pudo guardar la solicitud.';
          if (e.error?.mensaje)              mensaje = e.error.mensaje;
          else if (e.status === 400)         mensaje = 'Datos inválidos.';
          else if (e.status === 401 || e.status === 403) {
            mensaje = 'Sesión inválida.';
            localStorage.clear();
            setTimeout(() => this.router.navigate(['/login']), 2000);
          }
          else if (e.status === 0)           mensaje = 'Servidor no disponible.';
          this.mostrarToast('error', 'Error al Guardar', mensaje);
        }
      });
  }

  // ── Validación ────────────────────────────────────────────────────────
  validarFormulario(): boolean {
    if (!this.solicitud.id_carrera || !this.solicitud.id_materia || !this.solicitud.id_area) {
      this.mostrarToast('error', 'Campos Incompletos', 'Complete todos los campos obligatorios.');
      return false;
    }
    if (!this.solicitud.nivel_academico) {
      this.mostrarToast('error', 'Nivel Académico', 'Seleccione el nivel académico requerido.');
      return false;
    }
    if (this.solicitud.cantidad_docentes < 1) {
      this.mostrarToast('error', 'Cantidad', 'Debe solicitar al menos 1 docente.');
      return false;
    }
    if (this.solicitud.experiencia_profesional_min < 0) {
      this.mostrarToast('error', 'Experiencia Profesional', 'No puede ser negativa.');
      return false;
    }
    if (this.solicitud.experiencia_docente_min < 0) {
      this.mostrarToast('error', 'Experiencia Docente', 'No puede ser negativa.');
      return false;
    }
    if (!this.solicitud.justificacion || this.solicitud.justificacion.trim().length < 20) {
      this.mostrarToast('error', 'Justificación', 'Mínimo 20 caracteres.');
      return false;
    }
    return true;
  }

  cancelar(): void {
    const tieneContenido = this.solicitud.id_carrera || this.solicitud.justificacion;
    if (tieneContenido) {
      if (confirm('Se perderán los cambios. ¿Continuar?')) {
        this.router.navigate(['/evaluador']);
      }
    } else {
      this.router.navigate(['/evaluador']);
    }
  }

  crearSolicitudVacia() {
    return {
      id_carrera:                   null as number | null,
      id_area:                      null as number | null,
      id_materia:                   null as number | null,
      nivel_academico:              '',
      cantidad_docentes:             1,
      experiencia_profesional_min:   0,
      experiencia_docente_min:       0,
      justificacion:                '',
      // observaciones:                ''
    };
  }

  mostrarToast(tipo: 'success' | 'error', titulo: string, mensaje: string): void {
    this.toastType    = tipo;
    this.toastTitle   = titulo;
    this.toastMessage = mensaje;
    this.showToast    = true;
    clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => { this.showToast = false; }, 1800);
  }

  cerrarToast(): void {
    this.showToast = false;
    clearTimeout(this.toastTimer);
  }
}
