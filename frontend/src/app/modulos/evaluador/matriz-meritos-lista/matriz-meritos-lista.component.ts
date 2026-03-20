import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ModalEvaluadoresComponent } from '../../../component/modal-evaluadores.component';

export interface SolicitudItem {
  idSolicitud: number;
  materia: string;
  totalCandidatos: number;
  disponible: boolean;
  mensajeBloqueo: string | null;
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

  // Modal evaluadores
  modalEvaluadoresVisible = false;
  procesoSeleccionado: ProcesoItem | null = null;
  solicitudSeleccionada: SolicitudItem | null = null;
  cargandoProcesos = false;

  constructor(
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.cargarConvocatorias();
  }

  cargarConvocatorias(): void {
    this.cargando = true;
    this.error = '';

    this.http.get<any[]>(`${this.API}/convocatorias`).subscribe({
      next: (data) => {
        this.convocatorias = this.agrupar(data);
        this.cargando = false;
      },
      error: (err) => {
        this.error = err?.error?.mensaje || 'Error al cargar las convocatorias.';
        this.cargando = false;
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
        idSolicitud:     row.idSolicitud,
        materia:         row.materia,
        totalCandidatos: row.totalCandidatos,
        disponible:      row.disponible,
        mensajeBloqueo:  row.mensajeBloqueo,
        procesos:        []
      });
    }

    const result = Array.from(map.values());
    result.forEach(c => {
      c.tieneMultiples = c.solicitudes.length > 1;
      if (!c.tieneMultiples) c.expandida = true;
    });
    return result;
  }

  toggleExpandir(conv: ConvocatoriaAgrupada): void {
    if (conv.tieneMultiples) conv.expandida = !conv.expandida;
  }

  abrirMatriz(sol: SolicitudItem): void {
    if (!sol.disponible) return;
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
        // Si solo hay un proceso, abrir modal directo
        if (sol.procesos!.length === 1) {
          this.procesoSeleccionado = sol.procesos![0];
          this.modalEvaluadoresVisible = true;
        } else if (sol.procesos!.length > 1) {
          // Abrir con el primero por defecto — el modal muestra el nombre
          this.procesoSeleccionado = sol.procesos![0];
          this.modalEvaluadoresVisible = true;
        }
      },
      error: () => {
        // Si falla el endpoint específico, usar el primer proceso disponible
        this.cargandoProcesos = false;
        this.procesoSeleccionado = { idProceso: sol.idSolicitud, nombreCandidato: sol.materia };
        this.modalEvaluadoresVisible = true;
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
