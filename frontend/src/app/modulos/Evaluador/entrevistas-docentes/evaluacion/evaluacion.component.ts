// entrevistas-docentes/evaluacion/evaluacion.component.ts

import { Component, OnInit, AfterViewInit, ChangeDetectorRef, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { NavbarComponent } from '../../../../component/navbar';
import { EvaluacionService } from '../../../../services/entrevistas/evaluacion.service';
import { ReunionesService } from '../../../../services/entrevistas/reuniones.service';
import { ConfigCriteriosService } from '../../../../services/entrevistas/config-criterios.service';
import { EvaluacionRequest, CriterioResponse, ReunionResumen } from '../../../../models/entrevistas-models';

interface CriterioForm extends CriterioResponse {
  nota: number;
  estrellas: number;
  observacion: string;
}

@Component({
  selector: 'app-evaluacion',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './evaluacion.component.html',
  styleUrls: ['./evaluacion.component.scss']
})
export class EvaluacionComponent implements OnInit, AfterViewInit {

  reunion: ReunionResumen | null = null;
  criterios: CriterioForm[] = [];

  isLoading    = true;
  isSaving     = false;
  isConfirmado = false;
  error        = '';

  declaroSinConflicto = false;
  observaciones       = '';
  confirmado          = false;

  Math = Math;

  @ViewChild('canvas') canvasRef!: ElementRef<HTMLCanvasElement>;
  private isDrawing = false;
  private ctx!: CanvasRenderingContext2D;

  get calificacionFinalSobre5(): number {
    if (!this.criteriosCompletos) return 0;
    return this.criterios.reduce((s, c) => s + (c.nota * c.peso / 100), 0);
  }

  get calificacionFinal(): number {
    if (!this.criteriosCompletos) return 0;
    return this.criterios.reduce((s, c) => s + ((c.nota / 5) * c.peso), 0);
  }

  get criteriosCompletos(): boolean {
    return this.criterios.length > 0 && this.criterios.every(c => c.nota > 0);
  }

  get puedeConfirmar(): boolean {
    return this.criteriosCompletos && this.declaroSinConflicto
      && !!this.observaciones.trim() && this.confirmado;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private evaluacionService: EvaluacionService,
    private reunionesService: ReunionesService,
    private criteriosService: ConfigCriteriosService
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['idReunion']) this.cargarDatos(+params['idReunion']);
      else { this.error = 'No se especificó una reunión válida.'; this.isLoading = false; }
    });
  }

  ngAfterViewInit(): void { this.inicializarCanvas(); }

  cargarDatos(idReunion: number): void {
    this.isLoading = true;
    this.reunionesService.obtener(idReunion).subscribe({
      next: (reunion) => {
        this.reunion = reunion;
        // Cargar criterios de la fase de la reunión
        this.criteriosService.listarPorFase(reunion.idFase).subscribe({
          next: (criterios) => {
            this.criterios = criterios.map(c => ({ ...c, nota: 0, estrellas: 0, observacion: '' }));
            this.isLoading = false;
            this.cdr.detectChanges();
            setTimeout(() => this.inicializarCanvas(), 150);
          },
          error: (err) => {
            console.error(err); this.error = 'No se pudieron cargar los criterios.';
            this.isLoading = false; this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        console.error(err); this.error = 'No se pudo cargar la información de la reunión.';
        this.isLoading = false; this.cdr.detectChanges();
      }
    });
  }

  // ─── Canvas ──────────────────────────────────────────────────────────────

  inicializarCanvas(): void {
    if (!this.canvasRef) return;
    const canvas = this.canvasRef.nativeElement;
    this.ctx = canvas.getContext('2d')!;
    if (!this.ctx) return;
    this.ctx.strokeStyle = '#016630';
    this.ctx.lineWidth   = 2;
    this.ctx.lineCap     = 'round';

    canvas.addEventListener('mousedown',  (e) => { this.isDrawing = true; this.ctx.beginPath(); this.ctx.moveTo(e.offsetX, e.offsetY); });
    canvas.addEventListener('mousemove',  (e) => { if (!this.isDrawing) return; this.ctx.lineTo(e.offsetX, e.offsetY); this.ctx.stroke(); });
    canvas.addEventListener('mouseup',    ()  => { this.isDrawing = false; });
    canvas.addEventListener('mouseleave', ()  => { this.isDrawing = false; });
    canvas.addEventListener('touchstart', (e) => { e.preventDefault(); this.isDrawing = true; const r = canvas.getBoundingClientRect(); const t = e.touches[0]; this.ctx.beginPath(); this.ctx.moveTo(t.clientX - r.left, t.clientY - r.top); }, { passive: false });
    canvas.addEventListener('touchmove',  (e) => { e.preventDefault(); if (!this.isDrawing) return; const r = canvas.getBoundingClientRect(); const t = e.touches[0]; this.ctx.lineTo(t.clientX - r.left, t.clientY - r.top); this.ctx.stroke(); }, { passive: false });
    canvas.addEventListener('touchend',   ()  => { this.isDrawing = false; });
  }

  limpiarFirma(): void {
    if (this.ctx && this.canvasRef) {
      this.ctx.clearRect(0, 0, this.canvasRef.nativeElement.width, this.canvasRef.nativeElement.height);
    }
  }

  private capturarFirma(): string {
    return this.canvasRef?.nativeElement.toDataURL('image/png') ?? '';
  }

  // ─── Estrellas ────────────────────────────────────────────────────────────

  setEstrella(c: CriterioForm, valor: number): void { c.estrellas = valor; c.nota = valor; this.cdr.detectChanges(); }
  getEstrellas(n: number): number[] { return Array.from({ length: n }, (_, i) => i + 1); }

  // ─── Guardar ──────────────────────────────────────────────────────────────

  guardarBorrador(): void {
    localStorage.setItem(`ev_borrador_${this.reunion?.idReunion}`, JSON.stringify({
      criterios: this.criterios.map(c => ({ idCriterio: c.idCriterio, nota: c.nota, observacion: c.observacion })),
      observaciones: this.observaciones
    }));
    alert('✅ Borrador guardado localmente.');
  }

  confirmarEvaluacion(): void {
    if (!this.declaroSinConflicto)  { alert('Debe declarar que no tiene conflicto de interés.'); return; }
    if (!this.criteriosCompletos)   { alert('Debe calificar todos los criterios.'); return; }
    if (!this.observaciones.trim()) { alert('Las observaciones son obligatorias.'); return; }
    if (!this.confirmado)           { alert('Debe marcar la casilla de confirmación.'); return; }
    if (!this.reunion)              { return; }

    this.isSaving = true;

    const payload: EvaluacionRequest = {
      idReunion:           this.reunion.idReunion,
      criterios:           this.criterios.map(c => ({ idCriterio: c.idCriterio, nota: c.nota, observacion: c.observacion || undefined })),
      observaciones:       this.observaciones,
      declaroSinConflicto: this.declaroSinConflicto,
      firmaDigital:        this.capturarFirma()
    };

    this.evaluacionService.guardar(payload).subscribe({
      next: () => {
        this.isSaving = false; this.isConfirmado = true;
        localStorage.removeItem(`ev_borrador_${this.reunion?.idReunion}`);
        this.cdr.detectChanges();
        setTimeout(() => this.router.navigate(['/entrevistas-docentes/postulantes', this.reunion?.idProceso]), 2000);
      },
      error: (err) => {
        alert(err?.error?.mensaje || 'Error al guardar la evaluación.');
        this.isSaving = false; this.cdr.detectChanges();
      }
    });
  }
}
