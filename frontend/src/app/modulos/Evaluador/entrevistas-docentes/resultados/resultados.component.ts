// entrevistas-docentes/resultados/resultados.component.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../../../component/navbar';
import { ResultadosService } from '../../../../services/entrevistas/resultados.service';
import { PostulantesService } from '../../../../services/entrevistas/postulantes.service';
import { PostulanteResumen, ResultadoProceso, ResultadoFase, DecisionFinalRequest } from '../../../../models/entrevistas-models';

@Component({
  selector: 'app-resultados',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, DecimalPipe],
  templateUrl: './resultados.component.html',
  styleUrls: ['./resultados.component.scss']
})
export class ResultadosComponent implements OnInit {

  postulantes: PostulanteResumen[] = [];
  idProcesoSeleccionado = 0;
  resultado: ResultadoProceso | null = null;

  isLoading          = true;
  isLoadingResultado = false;
  isSaving           = false;
  error              = '';

  faseExpandida: number | null = null;

  decision      = '';
  justificacion = '';

  decisiones = [
    { value: 'aprobado_contratar', label: 'Aprobado – Contratar' },
    { value: 'aprobado_espera',    label: 'Aprobado – Lista de Espera' },
    { value: 'no_aprobado',        label: 'No Aprobado' },
    { value: 'segunda_ronda',      label: 'Requiere Segunda Ronda' }
  ];

  get fasesResultados(): ResultadoFase[] { return this.resultado?.fasesResultados ?? []; }
  get calificacionTotal(): number         { return this.resultado?.calificacionTotal ?? 0; }
  get progreso(): number                  { return this.resultado?.progreso ?? 0; }
  get procesoCompleto(): boolean {
    return this.fasesResultados.length > 0 && this.fasesResultados.every(f => f.estado === 'completada');
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private resultadosService: ResultadosService,
    private postulantesService: PostulantesService
  ) {}

  ngOnInit(): void {
    this.postulantesService.listar().subscribe({
      next: (data) => {
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
      error: (err) => {
        console.error(err); this.error = 'No se pudieron cargar los procesos.';
        this.isLoading = false; this.cdr.detectChanges();
      }
    });
  }

  cargarResultados(): void {
    if (!this.idProcesoSeleccionado) return;
    this.isLoadingResultado = true;
    this.resultado = null; this.faseExpandida = null;
    this.decision = ''; this.justificacion = '';

    this.resultadosService.obtenerResultados(this.idProcesoSeleccionado).subscribe({
      next: (data) => {
        this.resultado = data;
        if (data.decision) this.decision = data.decision;
        if (data.justificacionDecision) this.justificacion = data.justificacionDecision;
        this.isLoadingResultado = false; this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err); this.isLoadingResultado = false; this.cdr.detectChanges();
      }
    });
  }

  onProcesoChange(): void { this.cargarResultados(); }

  toggleFaseExpandida(id: number): void {
    this.faseExpandida = this.faseExpandida === id ? null : id;
  }

  irEvaluar(idReunion: number): void {
    this.router.navigate(['/entrevistas-docentes/evaluacion', idReunion]);
  }

  guardarDecision(): void {
    if (!this.decision)            { alert('Selecciona una decisión.'); return; }
    if (!this.justificacion.trim()) { alert('La justificación es obligatoria.'); return; }

    this.isSaving = true;
    const payload: DecisionFinalRequest = { decision: this.decision, justificacion: this.justificacion };

    this.resultadosService.guardarDecision(this.idProcesoSeleccionado, payload).subscribe({
      next: (data) => {
        this.resultado = data; this.isSaving = false;
        alert('✅ Decisión final guardada correctamente.'); this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err); alert('Error al guardar la decisión.');
        this.isSaving = false; this.cdr.detectChanges();
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
