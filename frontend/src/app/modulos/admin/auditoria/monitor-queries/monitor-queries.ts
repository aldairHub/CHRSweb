import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ToastComponent } from '../../../../component/toast.component';
import { ToastService } from '../../../../services/toast.service';

// ── Interfaces ───────────────────────────────────────────────────────────────

interface QueryMonitor {
  queryTexto:       string;
  llamadas:         number;
  tiempoPromedioMs: number;
  tiempoTotalMs:    number;
  filasPromedio:    number;
  porcentajeTiempo: number;
}

interface MonitorResumen {
  extensionDisponible:  boolean;
  totalQueriesUnicas:   number;
  queryMasLenta:        string;
  tiempoMasLento:       number;
  queryMasFrecuente:    string;
  llamadasMasFrecuente: number;
  queries:              QueryMonitor[];
}

@Component({
  selector: 'app-monitor-queries',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, ToastComponent],
  templateUrl: './monitor-queries.html',
  styleUrls: ['./monitor-queries.scss']
})
export class MonitorQueriesComponent implements OnInit {

  resumen:  MonitorResumen | null = null;
  loading          = false;
  ordenActual      = 'tiempo_promedio';  // 'tiempo_promedio' | 'tiempo_total' | 'llamadas'
  limite           = 20;

  // Modal confirmación reset
  modalResetAbierto = false;
  reseteando        = false;

  constructor(
    private http:  HttpClient,
    private toast: ToastService,
    private cdr:   ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(orden: string = this.ordenActual): void {
    this.ordenActual = orden;
    this.loading     = true;
    this.cdr.detectChanges();

    const params = new HttpParams()
      .set('orden',  orden)
      .set('limite', this.limite.toString());

    this.http.get<MonitorResumen>('/api/admin/auditoria/monitor/queries', { params })
      .subscribe({
        next: data => {
          this.resumen = data;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: err => {
          this.loading = false;
          this.cdr.detectChanges();
          this.toast.error('Error', 'Error al cargar monitor: ' + (err.error?.message || err.message));
        }
      });
  }

  abrirModalReset(): void  { this.modalResetAbierto = true; }
  cerrarModalReset(): void { this.modalResetAbierto = false; }

  confirmarReset(): void {
    this.reseteando = true;
    this.http.post('/api/admin/auditoria/monitor/resetear', {})
      .subscribe({
        next: () => {
          this.reseteando       = false;
          this.modalResetAbierto = false;
          this.toast.success('Estadísticas reiniciadas', 'Se reiniciaron las estadísticas de queries');
          this.cargar();
        },
        error: err => {
          this.reseteando = false;
          this.toast.error('Error', 'Error al resetear: ' + (err.error?.message || err.message));
        }
      });
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  colorTiempo(ms: number): string {
    if (ms < 10)  return 'verde';
    if (ms < 100) return 'amarillo';
    return 'rojo';
  }

  truncateQuery(q: string, largo = 80): string {
    if (!q || q.length <= largo) return q;
    return q.substring(0, largo) + '…';
  }

  formatMs(ms: number | null): string {
    if (ms === null || ms === undefined) return '—';
    return ms.toFixed(2) + ' ms';
  }
}
