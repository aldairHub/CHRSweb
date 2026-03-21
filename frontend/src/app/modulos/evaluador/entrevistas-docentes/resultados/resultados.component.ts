// ══════════════════════════════════════════════════════════════
// resultados.component.ts — SIN panel de decisión final
// Solo muestra resultados por fase y guarda puntaje de entrevista
// ══════════════════════════════════════════════════════════════
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ResultadosService } from '../../../../services/entrevistas/resultados.service';
import { PostulantesService } from '../../../../services/entrevistas/postulantes.service';
import { EntrevistasEstadoService } from '../../../../services/entrevistas/entrevistas-estado.service';
import { ToastService } from '../../../../services/toast.service';
import { PostulanteResumen, ResultadoProceso, ResultadoFase } from '../../../../models/entrevistas-models';

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, DecimalPipe],
  templateUrl: './resultados.component.html',
  styleUrls: ['./resultados.component.scss']
})
export class ResultadosComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/evaluacion/procesos';

  postulantes: PostulanteResumen[] = [];
  idProcesoSeleccionado = 0;
  resultado: ResultadoProceso | null = null;

  isLoading          = true;
  isLoadingResultado = false;
  isSaving           = false;
  error              = '';
  guardadoExitoso    = false;

  faseExpandida: number | null = null;

  get fasesResultados(): ResultadoFase[] { return this.resultado?.fasesResultados ?? []; }
  get calificacionTotal(): number         { return this.resultado?.calificacionTotal ?? 0; }
  get progreso(): number                  { return this.resultado?.progreso ?? 0; }
  get procesoCompleto(): boolean {
    return this.fasesResultados.length > 0 &&
      this.fasesResultados.every(f => f.estado === 'completada');
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private http: HttpClient,
    private resultadosService: ResultadosService,
    private postulantesService: PostulantesService,
    private estado: EntrevistasEstadoService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const idSolicitud = this.estado.getIdSolicitud();
    this.isLoading = true;
    this.cdr.detectChanges();
    this.postulantesService.listar(undefined, undefined, idSolicitud || undefined).subscribe({
      next: (data: PostulanteResumen[]) => {
        this.postulantes = data;
        this.isLoading   = false;
        this.route.params.subscribe(params => {
          if (params['idProceso']) {
            this.idProcesoSeleccionado = +params['idProceso'];
          } else if (data.length > 0) {
            this.idProcesoSeleccionado = data[0].idProceso;
          }
          if (this.idProcesoSeleccionado) this.cargarResultados();
        });
        this.cdr.detectChanges();
      },
      error: () => { this.isLoading = false; this.cdr.detectChanges(); }
    });
  }

  navegarPostulantes(): void {
    const id = this.estado.getIdSolicitud();
    if (id) this.router.navigate(['/evaluador/entrevistas-docentes/postulantes', id]);
    else    this.router.navigate(['/evaluador/entrevistas-docentes/postulantes']);
  }

  esRutaActiva(segmento: string): boolean { return this.router.url.includes(segmento); }

  cargarResultados(): void {
    if (!this.idProcesoSeleccionado) return;
    this.isLoadingResultado = true;
    this.resultado = null;
    this.faseExpandida = null;
    this.guardadoExitoso = false;
    this.cdr.detectChanges();

    this.resultadosService.obtenerResultados(this.idProcesoSeleccionado).subscribe({
      next: (data: ResultadoProceso) => {
        this.resultado = data;
        this.isLoadingResultado = false;
        this.cdr.detectChanges();
      },
      error: () => { this.isLoadingResultado = false; }
    });
  }

  onProcesoChange(): void { this.cargarResultados(); }

  toggleFaseExpandida(id: number): void {
    this.faseExpandida = this.faseExpandida === id ? null : id;
  }

  irEvaluar(idReunion: number): void {
    this.router.navigate(['/evaluador/entrevistas-docentes/evaluacion', idReunion]);
  }

  // Guardar puntaje de entrevista y disparar el 25% a la matriz
  guardarPuntajeEntrevista(): void {
    if (!this.procesoCompleto) {
      this.toast.warning('Proceso incompleto', 'Debe completar todas las fases antes de guardar el puntaje.');
      return;
    }

    this.isSaving = true;
    this.http.post(`${this.API}/${this.idProcesoSeleccionado}/finalizar-entrevista`, {}).subscribe({
      next: () => {
        this.isSaving = false;
        this.guardadoExitoso = true;
        this.cargarResultados();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.toast.error('Error al guardar', err?.error?.mensaje || 'No se pudo guardar el puntaje de entrevista.');
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  generarPDF(): void { this.resultadosService.abrirPDF(this.idProcesoSeleccionado); }

  getNombrePostulante(p: PostulanteResumen): string {
    return `${p.nombres} ${p.apellidos} – ${p.materia}`;
  }

  getBadgeClass(e: string): string {
    return { completada: 'success', programada: 'warning', bloqueada: 'default', pendiente: 'info' }[e] ?? 'default';
  }

  getBadgeLabel(e: string): string {
    return { completada: 'Completada', programada: 'Programada', bloqueada: 'Bloqueada', pendiente: 'Pendiente' }[e] ?? e;
  }

  getPromedioFase(f: ResultadoFase): number {
    if (!f.evaluaciones || f.evaluaciones.length === 0) return f.calificacion ?? 0;
    return f.evaluaciones.reduce((s, e) => s + e.calificacionFinal, 0) / f.evaluaciones.length;
  }
}
