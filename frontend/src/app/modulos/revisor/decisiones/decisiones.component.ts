// ══════════════════════════════════════════════════════════════
// decisiones.component.ts — Módulo Revisor
// Vista de decisiones del comité pendientes de revisión
// ══════════════════════════════════════════════════════════════
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

interface DecisionRevisor {
  id_decision: number;
  id_solicitud: number;
  nombre_materia: string;
  nombre_ganador: string;
  puntaje_final: number;
  acta_comite: string;
  estado: string;
  fecha_envio: string;
  expandida?: boolean;
}

@Component({
  selector: 'app-decisiones-revisor',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './decisiones.component.html',
  styleUrls: ['./decisiones.component.scss']
})
export class DecisionesRevisorComponent implements OnInit {

  private readonly API = 'http://localhost:8080/api/comite-final/revisor/decisiones';

  decisiones: DecisionRevisor[] = [];
  cargando = false;
  error = '';
  estadoFiltro: 'pendiente' | 'revisado' = 'pendiente';
  marcando: number | null = null;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.http.get<DecisionRevisor[]>(`${this.API}?estado=${this.estadoFiltro}`).subscribe({
      next: (data) => { this.decisiones = data; this.cargando = false; this.cdr.detectChanges(); },
      error: () => { this.error = 'Error al cargar las decisiones.'; this.cargando = false; }
    });
  }

  cambiarFiltro(estado: 'pendiente' | 'revisado'): void {
    this.estadoFiltro = estado;
    this.cargar();
  }

  toggleExpandir(d: DecisionRevisor): void { d.expandida = !d.expandida; }

  marcarRevisada(d: DecisionRevisor): void {
    if (!confirm(`¿Marcar la decisión sobre "${d.nombre_materia}" como revisada?`)) return;
    this.marcando = d.id_decision;

    this.http.patch(`${this.API}/${d.id_decision}/revisar`, {}).subscribe({
      next: () => {
        this.marcando = null;
        this.cargar();
      },
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
}
