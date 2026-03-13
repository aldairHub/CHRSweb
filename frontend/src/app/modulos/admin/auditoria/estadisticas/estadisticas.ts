import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ToastComponent } from '../../../../component/toast.component';
import { ToastService } from '../../../../services/toast.service';

interface TendenciaDia {
  fecha: string;
  total: number;
  exitosos: number;
  fallidos: number;
}

interface FallidoPorUsuario {
  usuario: string;
  intentos: number;
}

interface EstadisticasDto {
  totalRegistros: number;
  totalExitosos: number;
  totalFallidos: number;
  tasaExito: number;
  tendenciaSemanal: TendenciaDia[];
  topFallidosPorUsuario: FallidoPorUsuario[];
}

@Component({
  selector: 'app-estadisticas-auditoria',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, ToastComponent],
  templateUrl: './estadisticas.html',
  styleUrls: ['./estadisticas.scss']
})
export class EstadisticasAuditoriaComponent implements OnInit {

  private readonly apiUrl = 'http://localhost:8080/api/admin/auditoria';

  stats: EstadisticasDto | null = null;
  isLoading = true;
  chartMetric: 'todos' | 'exitosos' | 'fallidos' = 'todos';

  constructor(
    private http:  HttpClient,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.isLoading = true;
    this.http.get<EstadisticasDto>(`${this.apiUrl}/estadisticas`).subscribe({
      next: (data) => {
        this.stats     = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.toast.error('Error', 'No se pudieron cargar las estadísticas.');
        this.cdr.detectChanges();
      }
    });
  }

  formatFecha(fecha: string): string {
    return new Date(fecha + 'T00:00:00').toLocaleDateString('es-EC', {
      weekday: 'short', day: '2-digit', month: 'short'
    });
  }

  getBarHeight(value: number, max: number): string {
    if (!max) return '4px';
    const pct = Math.round((value / max) * 130);
    return Math.max(pct, 4) + 'px';
  }

  getBarWidth(value: number, max: number): string {
    if (!max) return '0%';
    return Math.round((value / max) * 100) + '%';
  }

  getMaxForMetric(): number {
    if (!this.stats?.tendenciaSemanal?.length) return 1;
    switch (this.chartMetric) {
      case 'exitosos': return Math.max(...this.stats.tendenciaSemanal.map(d => d.exitosos), 1);
      case 'fallidos': return Math.max(...this.stats.tendenciaSemanal.map(d => d.fallidos), 1);
      default:         return Math.max(...this.stats.tendenciaSemanal.map(d => d.total), 1);
    }
  }

  getMaxIntentos(): number {
    if (!this.stats?.topFallidosPorUsuario?.length) return 1;
    return Math.max(...this.stats.topFallidosPorUsuario.map(u => u.intentos), 1);
  }
}
