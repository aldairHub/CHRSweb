import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { FormsModule } from '@angular/forms';
import {
  DocumentoService,
  PostulanteInfo,
  ProgresoPostulante,
  FaseProgresoUI
} from '../../../services/documento.service';

// Fase con sus criterios evaluados — viene de /evaluacion/procesos/{id}/resultados
interface CriterioEvaluado {
  idCriterio: number;
  nombre:     string;
  peso:       number;
  nota:       number;
  observacion?: string;
}

interface EvaluacionFase {
  idEvaluacion:      number;
  nombreEvaluador:   string;
  criterios:         CriterioEvaluado[];
  calificacionFinal: number;
  observaciones:     string;
  fechaEvaluacion:   string;
}

interface FaseResultado {
  idFase:          number;
  nombreFase:      string;
  peso:            number;
  calificacion:    number | null;
  ponderado:       number | null;
  estado:          string;
  evaluadores:     string[];
  evaluaciones:    EvaluacionFase[];
}

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './resultados.html',
  styleUrls: ['./resultados.scss']
})
export class ResultadosComponent implements OnInit, OnDestroy {

  tabActiva: 'resumen' | 'meritos' | 'entrevista' | 'observaciones' | 'progreso' = 'progreso';
  cargandoResultados = true;
  cargandoProgreso2  = true;
  ultimaActualizacion: string | null = null;

  estado: 'en_proceso' | 'aprobado' | 'rechazado' = 'en_proceso';
  observaciones = '';

  // Fases del proceso con sus calificaciones reales (desde /evaluacion/procesos/{id}/resultados)
  fasesResultado: FaseResultado[] = [];

  // Puntaje de méritos (viene del progreso)
  puntajeMatriz: number | null = null;
  puntajeFinal:  number        = 0;

  // Progreso (tiempo real desde /progreso)
  progreso:        ProgresoPostulante | null = null;
  fasesProgreso:   FaseProgresoUI[]          = [];
  cargandoProgreso = true;
  errorProgreso:   string | null             = null;

  // idProceso obtenido del progreso — usado para llamar al endpoint de resultados
  private idProceso: number | null = null;

  get cargando(): boolean { return this.cargandoResultados || this.cargandoProgreso2; }
  get sinDatos(): boolean { return !this.cargandoProgreso && !this.progreso; }

  // Selector de convocatoria
  misPostulaciones:       PostulanteInfo[] = [];
  idPostulacionSeleccion: number | null    = null;
  get mostrarSelector(): boolean  { return this.misPostulaciones.length >= 1; }
  get selectorBloqueado(): boolean { return this.misPostulaciones.length <= 1; }

  private pollingSubscription: Subscription | null = null;
  private readonly POLLING_MS = 15_000;

  constructor(
    private router: Router,
    private documentoSvc: DocumentoService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idUsuario = Number(localStorage.getItem('idUsuario'));
    if (!idUsuario) { this.router.navigate(['/login']); return; }

    this.documentoSvc.listarMisPostulaciones(idUsuario).subscribe({
      next: lista => {
        this.misPostulaciones       = lista;
        this.idPostulacionSeleccion = lista.length > 0 ? lista[0].idPostulacion : null;
        this.iniciarCarga(idUsuario);
      },
      error: () => {
        this.idPostulacionSeleccion = null;
        this.iniciarCarga(idUsuario);
      }
    });
  }

  ngOnDestroy(): void { this.detenerPolling(); }

  private iniciarCarga(idUsuario: number): void {
    this.iniciarPolling(idUsuario);
  }

  private iniciarPolling(idUsuario: number): void {
    this.detenerPolling();
    this.cargarProgreso(idUsuario);
    this.pollingSubscription = interval(this.POLLING_MS).pipe(
      switchMap(() =>
        this.documentoSvc.obtenerMiProgreso(idUsuario, this.idPostulacionSeleccion ?? undefined)
      )
    ).subscribe({
      next: data => this.procesarProgreso(data),
      error: () => {}
    });
  }

  private detenerPolling(): void {
    this.pollingSubscription?.unsubscribe();
    this.pollingSubscription = null;
  }

  private cargarProgreso(idUsuario: number): void {
    this.documentoSvc.obtenerMiProgreso(idUsuario, this.idPostulacionSeleccion ?? undefined)
      .subscribe({
        next: data => {
          this.procesarProgreso(data);
          this.cargandoProgreso  = false;
          this.cargandoProgreso2 = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.errorProgreso     = 'No hay proceso de evaluación activo.';
          this.cargandoProgreso  = false;
          this.cargandoProgreso2 = false;
          this.cargandoResultados = false;
          this.cdr.detectChanges();
        }
      });
  }

  private procesarProgreso(data: ProgresoPostulante | null): void {
    if (!data || data.sinProceso) {
      this.errorProgreso      = 'El proceso de evaluación aún no ha iniciado para esta convocatoria.';
      this.cargandoProgreso   = false;
      this.cargandoProgreso2  = false;
      this.cargandoResultados = false;
      return;
    }

    this.progreso      = data;
    this.fasesProgreso = data.fases ?? [];
    this.errorProgreso = null;
    this.puntajeMatriz = data.puntajeMatriz ?? null;

    // Guardamos el idProceso para llamar al endpoint de resultados reales
    const nuevoIdProceso = (data as any).idProceso ?? null;
    if (nuevoIdProceso && nuevoIdProceso !== this.idProceso) {
      this.idProceso = nuevoIdProceso;
      this.cargarResultadosProceso(nuevoIdProceso);
    } else if (!nuevoIdProceso) {
      this.cargandoResultados = false;
    }

    // Estado
    const hayCompletadas = this.fasesProgreso.length > 0 &&
      this.fasesProgreso.every(f => f.estado === 'completada' || f.estado === 'omitida');
    const est = data.estadoGeneral ?? '';
    this.estado = (hayCompletadas || est === 'completado') ? 'aprobado'
      : est === 'rechazado' ? 'rechazado'
        : 'en_proceso';

    if (data.justificacion) this.observaciones = data.justificacion;

    this.ultimaActualizacion = new Date().toLocaleTimeString('es-EC', {
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
    this.cdr.detectChanges();
  }

  // Carga las fases con calificaciones reales desde el endpoint correcto
  private cargarResultadosProceso(idProceso: number): void {
    this.cargandoResultados = true;
    this.documentoSvc.obtenerResultadosProceso(idProceso).subscribe({
      next: (data: any) => {
        this.fasesResultado = (data.fasesResultados ?? []).map((f: any) => ({
          idFase:       f.idFase,
          nombreFase:   f.nombreFase,
          peso:         f.peso,
          calificacion: f.calificacion ?? null,
          ponderado:    f.ponderado    ?? null,
          estado:       f.estado       ?? 'pendiente',
          evaluadores:  f.evaluadores  ?? [],
          evaluaciones: (f.evaluaciones ?? []).map((e: any) => ({
            idEvaluacion:      e.idEvaluacion,
            nombreEvaluador:   e.nombreEvaluador,
            criterios:         e.criterios ?? [],
            calificacionFinal: e.calificacionFinal,
            observaciones:     e.observaciones ?? '',
            fechaEvaluacion:   e.fechaEvaluacion ?? '',
          }))
        }));

        // puntajeFinal: si todas las fases tienen calificacion, sumar ponderados
        const todasCalificadas = this.fasesResultado.every(f => f.calificacion !== null);
        if (todasCalificadas && this.fasesResultado.length > 0) {
          this.puntajeFinal = this.fasesResultado.reduce((s, f) => s + (f.ponderado ?? 0), 0);
        } else {
          this.puntajeFinal = 0;
        }

        this.cargandoResultados = false;
        this.cdr.detectChanges();
      },
      error: () => {
        // El endpoint falla si el proceso no tiene permisos — no bloqueante
        this.cargandoResultados = false;
        this.cdr.detectChanges();
      }
    });
  }

  onConvocatoriaChange(idPostulacion: number): void {
    if (this.selectorBloqueado) return;
    this.idPostulacionSeleccion = idPostulacion;
    this.idProceso              = null;
    this.cargandoProgreso       = true;
    this.cargandoProgreso2      = true;
    this.cargandoResultados     = true;
    this.progreso               = null;
    this.fasesResultado         = [];
    this.fasesProgreso          = [];
    this.puntajeFinal           = 0;
    this.puntajeMatriz          = null;

    const idUsuario = Number(localStorage.getItem('idUsuario'));
    this.iniciarPolling(idUsuario);
  }

  // ── Getters de presentación ───────────────────────────────────────────────

  get porcentajeProgreso(): number { return this.progreso?.progreso ?? 0; }

  get maximoFases(): number { return this.fasesResultado.reduce((s, f) => s + f.peso, 0); }

  get estadoGeneralEfectivo(): string {
    if (!this.progreso) return 'pendiente';
    if (this.porcentajeProgreso === 100) return 'completado';
    if (this.progreso.estadoGeneral === 'rechazado') return 'rechazado';
    if (this.porcentajeProgreso > 0) return 'en_proceso';
    return this.progreso.estadoGeneral ?? 'pendiente';
  }

  get estadoGeneralLabel(): string {
    const e = this.estadoGeneralEfectivo;
    return ({ pendiente: 'Pendiente', en_proceso: 'En proceso', completado: 'Completado', rechazado: 'No seleccionado' } as any)[e] ?? e;
  }

  get colorEstadoProgreso(): string {
    const e = this.estadoGeneralEfectivo;
    return ({ en_proceso: 'azul', completado: 'verde', rechazado: 'rojo' } as any)[e] ?? 'gris';
  }

  get colorEstado(): string {
    if (this.estado === 'aprobado')  return 'verde';
    if (this.estado === 'rechazado') return 'rojo';
    return 'amarillo';
  }

  iconoFase(estado: string): string {
    return ({ completada: 'pi-check-circle', en_curso: 'pi-spin pi-spinner', omitida: 'pi-minus-circle', pendiente: 'pi-circle', bloqueada: 'pi-lock' } as any)[estado] ?? 'pi-circle';
  }

  colorFase(estado: string): string {
    return ({ completada: 'verde', en_curso: 'azul', omitida: 'gris', pendiente: 'gris', bloqueada: 'gris' } as any)[estado] ?? 'gris';
  }

  estadoFaseLabel(estado: string): string {
    return ({ completada: 'Completada', en_curso: 'En curso', omitida: 'Omitida', pendiente: 'Pendiente', bloqueada: 'Pendiente' } as any)[estado] ?? estado;
  }

  get decisionLabel(): string {
    const d = this.progreso?.decision;
    if (!d) return '';
    return ({ aprobado_contratar: 'Aprobado — Proceder a contratación', aprobado_espera: 'Aprobado — Lista de espera', no_aprobado: 'No aprobado', segunda_ronda: 'Segunda ronda de entrevistas' } as any)[d] ?? d;
  }

  getBarWidth(p: number, m: number): number {
    if (!m) return 0;
    return Math.min(100, Math.round((p / m) * 100));
  }

  volver(): void { this.router.navigate(['/postulante']); }
}
