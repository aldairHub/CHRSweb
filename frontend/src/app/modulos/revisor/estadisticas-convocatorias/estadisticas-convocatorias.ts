import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { environment } from '../../../../environments/environment';

interface ConvocatoriaItem {
  idConvocatoria: number;
  titulo: string;
  estadoConvocatoria: string;
  fechaPublicacion: string;
  fechaInicio: string;
  fechaFin: string;
  fechaLimiteDocumentos: string | null;
  totalSolicitudes: number;
  documentosAbiertos: boolean;
}

interface Stats {
  total: number;
  abiertas: number;
  cerradas: number;
  canceladas: number;
  conDocumentosAbiertos: number;
  totalSolicitudesCubiertas: number;
  promedioSolicitudesPorConv: number;
  porMes: { mes: string; count: number }[];
  rankingSolicitudes: { titulo: string; total: number }[];
  duraciones: { titulo: string; dias: number }[];
  proximasACerrar: ConvocatoriaItem[];
}

@Component({
  selector: 'app-estadisticas-convocatorias',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ToastComponent],
  templateUrl: './estadisticas-convocatorias.html',
  styleUrls: ['./estadisticas-convocatorias.scss']
})
export class EstadisticasConvocatoriasComponent implements OnInit {

  private readonly API = environment.apiUrl;

  cargando = false;
  cargandoIA = false;
  analisisIA = '';
  datos: ConvocatoriaItem[] = [];
  stats: Stats | null = null;
  filtroEstado = '';
  ultimaActualizacion = '';

  sec = {
    kpis: true,
    estados: true,
    timeline: true,
    ranking: true,
    duracion: false,
    alertas: true,
  };

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef,
    private router: Router,
    private toast: ToastService
  ) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando = true;
    this.http.get<ConvocatoriaItem[]>(`${this.API}/admin/convocatorias`).subscribe({
      next: (data) => {
        this.datos = Array.isArray(data) ? data : [];
        this.calcularStats();
        this.cargando = false;
        this.ultimaActualizacion = new Date().toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' });
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('Error', 'No se pudieron cargar las convocatorias.');
        this.cdr.detectChanges();
      }
    });
  }

  private calcularStats(): void {
    const d = this.datos;
    const abiertas   = d.filter(c => c.estadoConvocatoria === 'abierta').length;
    const cerradas   = d.filter(c => c.estadoConvocatoria === 'cerrada').length;
    const canceladas = d.filter(c => c.estadoConvocatoria === 'cancelada').length;
    const conDocs    = d.filter(c => c.documentosAbiertos).length;
    const totalSolic = d.reduce((s, c) => s + (c.totalSolicitudes ?? 0), 0);

    // Agrupación por mes de publicación
    const byMes: Record<string, number> = {};
    d.forEach(c => {
      if (!c.fechaPublicacion) return;
      const m = c.fechaPublicacion.slice(0, 7); // yyyy-MM
      byMes[m] = (byMes[m] ?? 0) + 1;
    });
    const porMes = Object.entries(byMes)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-8)
      .map(([mes, count]) => ({ mes, count }));

    // Ranking por solicitudes cubiertas
    const rankingSolicitudes = [...d]
      .filter(c => c.totalSolicitudes > 0)
      .sort((a, b) => b.totalSolicitudes - a.totalSolicitudes)
      .slice(0, 7)
      .map(c => ({ titulo: c.titulo, total: c.totalSolicitudes }));

    // Duraciones de convocatorias cerradas (días entre inicio y fin)
    const duraciones = d
      .filter(c => c.fechaInicio && c.fechaFin)
      .map(c => {
        const dias = Math.round(
          (new Date(c.fechaFin).getTime() - new Date(c.fechaInicio).getTime()) / 86400000
        );
        return { titulo: c.titulo, dias };
      })
      .sort((a, b) => b.dias - a.dias)
      .slice(0, 6);

    // Próximas a cerrar (abiertas con fechaFin en los próximos 14 días)
    const hoy = new Date();
    const en14 = new Date(hoy.getTime() + 14 * 86400000);
    const proximasACerrar = d
      .filter(c => c.estadoConvocatoria === 'abierta' && c.fechaFin)
      .filter(c => new Date(c.fechaFin) <= en14)
      .sort((a, b) => new Date(a.fechaFin).getTime() - new Date(b.fechaFin).getTime())
      .slice(0, 5);

    this.stats = {
      total: d.length,
      abiertas, cerradas, canceladas,
      conDocumentosAbiertos: conDocs,
      totalSolicitudesCubiertas: totalSolic,
      promedioSolicitudesPorConv: d.length ? Math.round(totalSolic / d.length * 10) / 10 : 0,
      porMes,
      rankingSolicitudes,
      duraciones,
      proximasACerrar,
    };
  }

  generarIA(): void {
    if (this.cargandoIA || !this.stats) return;
    this.cargandoIA = true;
    this.analisisIA = '';
    this.http.post<{ analisis: string }>(`${this.API}/revisor/reportes/analisis-ia`, {
      convocatorias: this.stats,
    }).subscribe({
      next: r => { this.analisisIA = r.analisis ?? this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); },
      error: ()  => { this.analisisIA = this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); }
    });
  }

  private fallbackIA(): string {
    const s = this.stats!;
    const pcts = s.total ? Math.round(s.abiertas / s.total * 100) : 0;
    let txt = `El sistema registra ${s.total} convocatoria${s.total !== 1 ? 's' : ''} en total. `;
    txt += `Actualmente ${s.abiertas} (${pcts}%) se encuentran abiertas, ${s.cerradas} cerradas y ${s.canceladas} canceladas. `;
    if (s.totalSolicitudesCubiertas > 0)
      txt += `Se han cubierto ${s.totalSolicitudesCubiertas} solicitudes de docente con un promedio de ${s.promedioSolicitudesPorConv} por convocatoria. `;
    if (s.proximasACerrar.length > 0)
      txt += `⚠ Hay ${s.proximasACerrar.length} convocatoria${s.proximasACerrar.length > 1 ? 's' : ''} que cierra${s.proximasACerrar.length > 1 ? 'n' : ''} en los próximos 14 días. `;
    if (s.abiertas === 0)
      txt += 'No existen convocatorias abiertas actualmente; se recomienda evaluar la apertura de nuevos procesos. ';
    return txt.trim();
  }

  toggle(k: keyof typeof this.sec) { this.sec[k] = !this.sec[k]; }

  barWidth(v: number, max: number): string {
    return (!max ? 0 : Math.max(Math.round(v / max * 100), 2)) + '%';
  }
  barHeight(v: number, max: number): string {
    return (!max ? 4 : Math.max(Math.round(v / max * 120), 4)) + 'px';
  }
  maxMes(): number { return Math.max(...(this.stats?.porMes.map(m => m.count) ?? [1]), 1); }
  maxRanking(): number { return Math.max(...(this.stats?.rankingSolicitudes.map(r => r.total) ?? [1]), 1); }
  maxDuracion(): number { return Math.max(...(this.stats?.duraciones.map(d => d.dias) ?? [1]), 1); }

  colorEstado(e: string) {
    return ({ abierta: '#16a34a', cerrada: '#6b7280', cancelada: '#dc2626' } as any)[e] ?? '#6b7280';
  }
  labelEstado(e: string) {
    return ({ abierta: 'Abierta', cerrada: 'Cerrada', cancelada: 'Cancelada' } as any)[e] ?? e;
  }
  classBadge(e: string) {
    return ({ abierta: 'badge-ok', cerrada: 'badge-neutral', cancelada: 'badge-danger' } as any)[e] ?? '';
  }
  diasRestantes(fechaFin: string): number {
    return Math.round((new Date(fechaFin).getTime() - Date.now()) / 86400000);
  }
  formatMes(m: string): string {
    const [y, mo] = m.split('-');
    const nombres = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
    return (nombres[+mo - 1] ?? mo) + ' ' + y.slice(2);
  }
  donutOffset(pct: number): number { return 175.9 - 175.9 * Math.max(0, Math.min(100, pct)) / 100; }

  volver() { this.router.navigate(['/revisor/convocatorias']); }
  irReportes() { this.router.navigate(['/revisor/reportes']); }
}
