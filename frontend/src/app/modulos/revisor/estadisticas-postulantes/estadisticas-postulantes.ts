import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { environment } from '../../../../environments/environment';

interface PostulanteItem {
  idPrepostulacion: number;
  nombres: string;
  apellidos: string;
  identificacion: string;
  correo: string;
  estadoRevision: string;
  fechaEnvio: string;
  idConvocatoria: number | null;
  idSolicitud: number | null;
}

interface Stats {
  total: number;
  aprobados: number;
  rechazados: number;
  pendientes: number;
  tasaAprobacion: number;
  tasaRechazo: number;
  conDocumentosCompletos: number;
  porDia: { dia: string; total: number; aprobados: number; rechazados: number }[];
  porConvocatoria: { id: number; count: number; aprobados: number; rechazados: number }[];
  tiempoPromedioRevision: number;
  ultimosIngresados: PostulanteItem[];
}

@Component({
  selector: 'app-estadisticas-postulantes',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './estadisticas-postulantes.html',
  styleUrls: ['./estadisticas-postulantes.scss']
})
export class EstadisticasPostulantesComponent implements OnInit {

  private readonly API = environment.apiUrl;

  cargando = false;
  cargandoIA = false;
  analisisIA = '';
  datos: PostulanteItem[] = [];
  stats: Stats | null = null;
  ultimaActualizacion = '';
  metricaTendencia: 'total' | 'aprobados' | 'rechazados' = 'total';

  sec = {
    kpis: true,
    funnel: true,
    tendencia: true,
    porConv: true,
    recientes: false,
    ia: true,
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
    this.http.get<PostulanteItem[]>(`${this.API}/admin/prepostulaciones`).subscribe({
      next: data => {
        this.datos = Array.isArray(data) ? data : [];
        this.calcularStats();
        this.cargando = false;
        this.ultimaActualizacion = new Date().toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' });
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('Error', 'No se pudieron cargar las prepostulaciones.');
        this.cdr.detectChanges();
      }
    });
  }

  private calcularStats(): void {
    const d = this.datos;
    const aprobados  = d.filter(p => p.estadoRevision === 'APROBADO').length;
    const rechazados = d.filter(p => p.estadoRevision === 'RECHAZADO').length;
    const pendientes = d.filter(p => p.estadoRevision === 'PENDIENTE').length;
    const revisados  = aprobados + rechazados;
    const tasaAprobacion = revisados > 0 ? Math.round(aprobados  / revisados * 100) : 0;
    const tasaRechazo    = revisados > 0 ? Math.round(rechazados / revisados * 100) : 0;

    // Por día (últimos 14 días)
    const byDia: Record<string, { total: number; aprobados: number; rechazados: number }> = {};
    d.forEach(p => {
      if (!p.fechaEnvio) return;
      const dia = p.fechaEnvio.slice(0, 10);
      if (!byDia[dia]) byDia[dia] = { total: 0, aprobados: 0, rechazados: 0 };
      byDia[dia].total++;
      if (p.estadoRevision === 'APROBADO')  byDia[dia].aprobados++;
      if (p.estadoRevision === 'RECHAZADO') byDia[dia].rechazados++;
    });
    const porDia = Object.entries(byDia)
      .sort(([a], [b]) => a.localeCompare(b))
      .slice(-12)
      .map(([dia, v]) => ({ dia, ...v }));

    // Por convocatoria
    const byConv: Record<number, { id: number; count: number; aprobados: number; rechazados: number }> = {};
    d.forEach(p => {
      const id = p.idConvocatoria ?? 0;
      if (!byConv[id]) byConv[id] = { id, count: 0, aprobados: 0, rechazados: 0 };
      byConv[id].count++;
      if (p.estadoRevision === 'APROBADO')  byConv[id].aprobados++;
      if (p.estadoRevision === 'RECHAZADO') byConv[id].rechazados++;
    });
    const porConvocatoria = Object.values(byConv)
      .sort((a, b) => b.count - a.count)
      .slice(0, 7);

    // Últimos 5 ingresados
    const ultimosIngresados = [...d]
      .filter(p => p.fechaEnvio)
      .sort((a, b) => b.fechaEnvio.localeCompare(a.fechaEnvio))
      .slice(0, 5);

    // Tiempo promedio de revisión (días entre fechaEnvio y hoy para los revisados, aproximado)
    const ahora = Date.now();
    const tiempos = d
      .filter(p => p.fechaEnvio && p.estadoRevision !== 'PENDIENTE')
      .map(p => (ahora - new Date(p.fechaEnvio).getTime()) / 86400000);
    const tiempoPromedioRevision = tiempos.length
      ? Math.round(tiempos.reduce((s, t) => s + t, 0) / tiempos.length * 10) / 10
      : 0;

    this.stats = {
      total: d.length, aprobados, rechazados, pendientes,
      tasaAprobacion, tasaRechazo,
      conDocumentosCompletos: aprobados,
      porDia, porConvocatoria,
      tiempoPromedioRevision,
      ultimosIngresados,
    };
  }

  generarIA(): void {
    if (this.cargandoIA || !this.stats) return;
    this.cargandoIA = true; this.analisisIA = '';
    this.http.post<{ analisis: string }>(`${this.API}/revisor/reportes/analisis-ia`, {
      prepostulaciones: this.stats
    }).subscribe({
      next: r => { this.analisisIA = r.analisis ?? this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); },
      error: () => { this.analisisIA = this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); }
    });
  }

  private fallbackIA(): string {
    const s = this.stats!;
    let t = `Se registran ${s.total} prepostulación${s.total !== 1 ? 'es' : ''} en total. `;
    t += `La tasa de aprobación sobre revisadas es del ${s.tasaAprobacion}%, con ${s.aprobados} aprobadas y ${s.rechazados} rechazadas. `;
    if (s.pendientes > 0) t += `Quedan ${s.pendientes} pendiente${s.pendientes > 1 ? 's' : ''} de revisión. `;
    if (s.tiempoPromedioRevision > 0) t += `El tiempo promedio desde el envío hasta la revisión es de ${s.tiempoPromedioRevision} días. `;
    if (s.tasaAprobacion < 50 && s.total > 5) t += 'La tasa de aprobación inferior al 50% sugiere revisar los criterios de admisión o reforzar la orientación previa a los postulantes. ';
    if (s.pendientes > s.aprobados + s.rechazados) t += 'El alto volumen de pendientes requiere atención prioritaria para no retrasar el proceso de selección. ';
    return t.trim();
  }

  toggle(k: keyof typeof this.sec) { this.sec[k] = !this.sec[k]; }

  barWidth(v: number, max: number) { return (!max ? 0 : Math.max(Math.round(v / max * 100), 2)) + '%'; }
  barHeight(v: number, max: number) { return (!max ? 4 : Math.max(Math.round(v / max * 110), 4)) + 'px'; }

  get maxTendencia(): number {
    const f = this.metricaTendencia;
    return Math.max(...(this.stats?.porDia.map(d => (d as any)[f]) ?? [1]), 1);
  }
  maxConv() { return Math.max(...(this.stats?.porConvocatoria.map(c => c.count) ?? [1]), 1); }

  tendenciaVal(d: any): number { return (d as any)[this.metricaTendencia]; }
  colorTendencia(): string {
    return ({ total: '#2563eb', aprobados: '#16a34a', rechazados: '#dc2626' } as any)[this.metricaTendencia];
  }

  donutOffset(pct: number) { return 175.9 - 175.9 * Math.max(0, Math.min(100, pct)) / 100; }
  formatFecha(f: string) {
    if (!f) return '';
    return new Date(f + 'T00:00:00').toLocaleDateString('es-EC', { day: '2-digit', month: 'short' });
  }
  labelEstado(e: string) {
    return ({ APROBADO: 'Aprobado', RECHAZADO: 'Rechazado', PENDIENTE: 'Pendiente' } as any)[e] ?? e;
  }
  classBadge(e: string) {
    return ({ APROBADO: 'badge-ok', RECHAZADO: 'badge-danger', PENDIENTE: 'badge-warn' } as any)[e] ?? '';
  }

  volver() { this.router.navigate(['/revisor/prepostulaciones']); }
  irReportes() { this.router.navigate(['/revisor/reportes']); }
}
