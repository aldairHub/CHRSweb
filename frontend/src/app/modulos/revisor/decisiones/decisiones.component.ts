// ════════════════════════════════════════════════════════════════════════
// decisiones.component.ts — Módulo Revisor (ACTUALIZADO)
// ════════════════════════════════════════════════════════════════════════
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface FaseEntrevista {
  nombre_fase:      string;
  puntaje_obtenido: number;
  observaciones:    string;
  estado:           string;
}

interface SeccionDetalle {
  seccion:        string;
  puntaje_maximo: number;
  subtotal:       number;
}

interface CandidatoDetalle {
  nombre:              string;
  decision_comite:     string;
  puntaje_meritos:     number;
  puntaje_entrevista:  number;
  puntaje_total:       number;
  justificacion_decision: string;
  habilitado_entrevista:  boolean;
  fases_entrevista:    FaseEntrevista[];
  detalle_secciones:   SeccionDetalle[];
  expandido?:          boolean;
}

interface DecisionRevisor {
  id_decision:     number;
  id_solicitud:    number;
  nombre_materia:  string;
  nombre_ganador:  string;
  puntaje_final:   number;
  acta_comite:     string;
  estado:          string;
  fecha_envio:     string;
  expandida?:      boolean;
  // Detalle cargado bajo demanda
  detalle?:        any;
  cargandoDetalle?: boolean;
}

@Component({
  selector: 'app-decisiones-revisor',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './decisiones.component.html',
  styleUrls: ['./decisiones.component.scss']
})
export class DecisionesRevisorComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/comite-final';

  decisiones: DecisionRevisor[] = [];
  cargando    = false;
  error       = '';
  estadoFiltro: 'pendiente' | 'revisado' = 'pendiente';
  marcando: number | null = null;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando = true;
    this.error    = '';
    this.http.get<DecisionRevisor[]>(
      `${this.API}/revisor/decisiones?estado=${this.estadoFiltro}`
    ).subscribe({
      next:  (data) => { this.decisiones = data; this.cargando = false; this.cdr.detectChanges(); },
      error: ()     => { this.error = 'Error al cargar las decisiones.'; this.cargando = false; }
    });
  }

  cambiarFiltro(estado: 'pendiente' | 'revisado'): void {
    this.estadoFiltro = estado;
    this.cargar();
  }

  toggleExpandir(d: DecisionRevisor): void {
    d.expandida = !d.expandida;
    if (d.expandida && !d.detalle && !d.cargandoDetalle) {
      this.cargarDetalle(d);
    }
  }

  cargarDetalle(d: DecisionRevisor): void {
    d.cargandoDetalle = true;
    this.http.get<any>(
      `${this.API}/revisor/decisiones/${d.id_solicitud}/detalle`
    ).subscribe({
      next:  (data) => { d.detalle = data; d.cargandoDetalle = false; this.cdr.detectChanges(); },
      error: ()     => { d.cargandoDetalle = false; this.cdr.detectChanges(); }
    });
  }

  toggleCandidato(cand: CandidatoDetalle): void {
    cand.expandido = !cand.expandido;
  }

  marcarRevisada(d: DecisionRevisor): void {
    if (!confirm(`¿Marcar la decisión sobre "${d.nombre_materia}" como revisada?`)) return;
    this.marcando = d.id_decision;
    this.http.patch(`${this.API}/revisor/decisiones/${d.id_decision}/revisar`, {}).subscribe({
      next:  () => { this.marcando = null; this.cargar(); },
      error: () => { this.marcando = null; }
    });
  }

  getFechaFormateada(fecha: string): string {
    if (!fecha) return '—';
    try {
      return new Date(fecha).toLocaleDateString('es-EC', {
        day: '2-digit', month: 'long', year: 'numeric',
        hour: '2-digit', minute: '2-digit'
      });
    } catch { return fecha; }
  }

  getBarWidth(valor: number, maximo: number): number {
    if (!maximo) return 0;
    return Math.min(Math.round((valor / maximo) * 100), 100);
  }
}
