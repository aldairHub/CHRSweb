import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ModalEvaluadoresComponent } from '../../../component/modal-evaluadores.component';
import { AuthStateService } from '../../../services/auth-state.service';

export interface SolicitudItem {
  idSolicitud: number;
  materia: string;
  totalCandidatos: number;
  disponible: boolean;
  mensajeBloqueo: string | null;
  idUsuarioSolicitante: number | null;
  idFacultadSolicitud: number | null;
  procesos?: ProcesoItem[];
}

export interface ProcesoItem {
  idProceso: number;
  nombreCandidato: string;
}

export interface ConvocatoriaAgrupada {
  idConvocatoria: number;
  titulo: string;
  fechaLimiteDocumentos: string | null;
  solicitudes: SolicitudItem[];
  expandida: boolean;
  tieneMultiples: boolean;
}

@Component({
  selector: 'app-matriz-meritos-lista',
  standalone: true,
  imports: [CommonModule, ModalEvaluadoresComponent],
  templateUrl: './matriz-meritos-lista.component.html',
  styleUrls: ['./matriz-meritos-lista.component.scss']
})
export class MatrizMeritosListaComponent implements OnInit {

  private readonly API       = 'http://localhost:8080/api/matriz-meritos';
  private readonly API_EVAL  = 'http://localhost:8080/api/evaluadores-asignados';

  cargando = false;
  error = '';
  convocatorias: ConvocatoriaAgrupada[] = [];
  idUsuarioLogueado: number | null = null;

  // Modal evaluadores
  modalEvaluadoresVisible = false;
  procesoSeleccionado: ProcesoItem | null = null;
  solicitudSeleccionada: SolicitudItem | null = null;
  cargandoProcesos = false;

  constructor(
    private router: Router,
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private authState: AuthStateService
  ) {}

  ngOnInit(): void {
    this.idUsuarioLogueado = this.authState.getEstado().idUsuario;
    this.cargarConvocatorias();
  }

  cargarConvocatorias(): void {
    this.cargando = true;
    this.error = '';

    this.http.get<any[]>(`${this.API}/convocatorias`).subscribe({
      next: (data) => {
        this.convocatorias = this.agrupar(data);
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar las convocatorias.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  private agrupar(rows: any[]): ConvocatoriaAgrupada[] {
    const map = new Map<number, ConvocatoriaAgrupada>();

    for (const row of rows) {
      if (!map.has(row.idConvocatoria)) {
        map.set(row.idConvocatoria, {
          idConvocatoria:        row.idConvocatoria,
          titulo:                row.titulo,
          fechaLimiteDocumentos: row.fechaLimiteDocumentos,
          solicitudes:           [],
          expandida:             false,
          tieneMultiples:        false
        });
      }

      const conv = map.get(row.idConvocatoria)!;
      conv.solicitudes.push({
        idSolicitud:          row.idSolicitud,
        materia:              row.materia,
        totalCandidatos:      row.totalCandidatos,
        disponible:           row.disponible,
        mensajeBloqueo:       row.mensajeBloqueo,
        idUsuarioSolicitante: row.idUsuarioSolicitante ?? null,
        idFacultadSolicitud:  row.idFacultadSolicitud  ?? null,
        procesos:             []
      });
    }

    const result = Array.from(map.values());
    result.forEach(c => {
      c.tieneMultiples = c.solicitudes.length > 1;
      if (!c.tieneMultiples) c.expandida = true;
    });
    return result;
  }

  esSolicitante(sol: SolicitudItem): boolean {
    if (!sol.idUsuarioSolicitante || !this.idUsuarioLogueado) return false;
    return sol.idUsuarioSolicitante === this.idUsuarioLogueado;
  }

  toggleExpandir(conv: ConvocatoriaAgrupada): void {
    if (conv.tieneMultiples) conv.expandida = !conv.expandida;
  }

  abrirMatriz(sol: SolicitudItem): void {
    if (!sol.disponible) return;
    if (!this.esSolicitante(sol)) return;
    this.router.navigate(['/evaluador/matriz-meritos', sol.idSolicitud]);
  }

  algunaDisponible(conv: ConvocatoriaAgrupada): boolean {
    return conv.solicitudes.some(s => s.disponible);
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
  }

  // ── Gestión de evaluadores ────────────────────────────────

  abrirGestionEvaluadores(sol: SolicitudItem, event: Event): void {
    event.stopPropagation();
    this.solicitudSeleccionada = sol;
    this.cargandoProcesos = true;

    // Cargar los procesos de esta solicitud para poder asignar evaluadores
    this.http.get<any[]>(`${this.API_EVAL}/solicitud/${sol.idSolicitud}/procesos`).subscribe({
      next: (procesos) => {
        sol.procesos = procesos.map(p => ({
          idProceso: p.idProceso,
          nombreCandidato: p.apellidos + ' ' + p.nombres
        }));
        this.cargandoProcesos = false;
        if (sol.procesos!.length === 1) {
          this.procesoSeleccionado = sol.procesos![0];
          this.modalEvaluadoresVisible = true;
        } else if (sol.procesos!.length > 1) {
          this.procesoSeleccionado = sol.procesos![0];
          this.modalEvaluadoresVisible = true;
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoProcesos = false;
        this.procesoSeleccionado = { idProceso: sol.idSolicitud, nombreCandidato: sol.materia };
        this.modalEvaluadoresVisible = true;
        this.cdr.detectChanges();
      }
    });
  }

  cerrarModalEvaluadores(): void {
    this.modalEvaluadoresVisible = false;
    this.procesoSeleccionado = null;
    this.solicitudSeleccionada = null;
  }

  get contextLabelEvaluadores(): string {
    if (!this.solicitudSeleccionada) return '';
    return `${this.solicitudSeleccionada.materia} — Matriz de Méritos`;
  }
}
