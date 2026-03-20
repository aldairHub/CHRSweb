import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { environment } from '../../../../environments/environment';

interface ConvocatoriaItem {
  idConvocatoria: number; titulo: string; estadoConvocatoria: string;
  fechaPublicacion: string; fechaInicio: string; fechaFin: string;
  fechaLimiteDocumentos: string | null; totalSolicitudes: number; documentosAbiertos: boolean;
}

interface Stats {
  total: number; abiertas: number; cerradas: number; canceladas: number;
  conDocumentosAbiertos: number; totalSolicitudesCubiertas: number;
  promedioSolicitudesPorConv: number; tasaExito: number; eficienciaCobertura: number;
  convMasSolicitudes: string; promedioDuracion: number;
  activas: number; vencidasSinCerrar: number; convSinSolicitudes: number;
  convMasLarga: { titulo: string; dias: number };
  convMasCorta: { titulo: string; dias: number };
  porMes: { mes: string; count: number; cerradas: number }[];
  rankingSolicitudes: { titulo: string; total: number; estado: string }[];
  duraciones: { titulo: string; dias: number; estado: string }[];
  proximasACerrar: ConvocatoriaItem[];
}

@Component({
  selector: 'app-estadisticas-convocatorias',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './estadisticas-convocatorias.html',
  styleUrls: ['./estadisticas-convocatorias.scss']
})
export class EstadisticasConvocatoriasComponent implements OnInit {

  private readonly API = environment.apiUrl;
  cargando = false; cargandoIA = false; analisisIA = '';
  datos: ConvocatoriaItem[] = []; stats: Stats | null = null; ultimaAct = '';

  showExport = false; exportando = false;
  exportCfg = {
    formato: 'PDF' as 'PDF'|'EXCEL', orientacion: 'HORIZONTAL' as 'VERTICAL'|'HORIZONTAL',
    titulo: 'Estadísticas de Convocatorias', subtitulo: '', colorPrimario: '#00A63E',
    estado: '', desde: '', hasta: '',
    incluirPortada: true, incluirKpis: true, incluirDetalle: true,
    incluirGraficoEstados: true, incluirGraficoPrepostulaciones: true, incluirGraficoTemporal: true,
    mostrarNumeroPagina: true, mostrarFechaGeneracion: true,
    excelCongelarEncabezado: true, excelFiltrosAutomaticos: true,
  };

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef,
              private router: Router, private toast: ToastService) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando = true;
    this.http.get<ConvocatoriaItem[]>(`${this.API}/admin/convocatorias`).subscribe({
      next: d => {
        this.datos = Array.isArray(d) ? d : [];
        this.calcular();
        this.cargando = false;
        this.ultimaAct = new Date().toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' });
        this.cdr.detectChanges();
      },
      error: () => { this.cargando = false; this.toast.error('Error', 'No se pudieron cargar las convocatorias.'); this.cdr.detectChanges(); }
    });
  }

  private calcular(): void {
    const d = this.datos;
    if (!d.length) { this.stats = null; return; }
    const abiertas = d.filter(c => c.estadoConvocatoria === 'abierta').length;
    const cerradas = d.filter(c => c.estadoConvocatoria === 'cerrada').length;
    const canceladas = d.filter(c => c.estadoConvocatoria === 'cancelada').length;
    const totalSolic = d.reduce((s, c) => s + (c.totalSolicitudes ?? 0), 0);
    const conSolic = d.filter(c => c.totalSolicitudes > 0).length;
    const eficienciaCobertura = d.length ? Math.round(conSolic / d.length * 100) : 0;
    const cerradasConSolic = d.filter(c => c.estadoConvocatoria === 'cerrada' && c.totalSolicitudes > 0).length;
    const tasaExito = cerradas > 0 ? Math.round(cerradasConSolic / cerradas * 100) : 0;
    const topSolic = [...d].sort((a, b) => b.totalSolicitudes - a.totalSolicitudes)[0];
    const conDuracion = d.filter(c => c.fechaInicio && c.fechaFin).map(c => ({
      titulo: c.titulo, estado: c.estadoConvocatoria,
      dias: Math.round((new Date(c.fechaFin).getTime() - new Date(c.fechaInicio).getTime()) / 86400000),
    })).filter(c => c.dias > 0);
    const promedioDuracion = conDuracion.length
      ? Math.round(conDuracion.reduce((s, c) => s + c.dias, 0) / conDuracion.length) : 0;
    const sorted = [...conDuracion].sort((a, b) => b.dias - a.dias);
    const hoy = new Date();
    const activas = d.filter(c => c.estadoConvocatoria === 'abierta' && c.fechaFin && new Date(c.fechaFin) >= hoy).length;
    const vencidasSinCerrar = d.filter(c => c.estadoConvocatoria === 'abierta' && c.fechaFin && new Date(c.fechaFin) < hoy).length;
    const byMes: Record<string, { count: number; cerradas: number }> = {};
    d.forEach(c => {
      if (!c.fechaPublicacion) return;
      const m = c.fechaPublicacion.slice(0, 7);
      if (!byMes[m]) byMes[m] = { count: 0, cerradas: 0 };
      byMes[m].count++;
      if (c.estadoConvocatoria === 'cerrada') byMes[m].cerradas++;
    });
    const porMes = Object.entries(byMes).sort(([a], [b]) => a.localeCompare(b)).slice(-10)
      .map(([mes, v]) => ({ mes, ...v }));
    const en14 = new Date(hoy.getTime() + 14 * 86400000);
    const proximasACerrar = d
      .filter(c => c.estadoConvocatoria === 'abierta' && c.fechaFin && new Date(c.fechaFin) <= en14 && new Date(c.fechaFin) >= hoy)
      .sort((a, b) => new Date(a.fechaFin).getTime() - new Date(b.fechaFin).getTime()).slice(0, 5);

    this.stats = {
      total: d.length, abiertas, cerradas, canceladas,
      conDocumentosAbiertos: d.filter(c => c.documentosAbiertos).length,
      totalSolicitudesCubiertas: totalSolic, promedioSolicitudesPorConv: d.length ? +(totalSolic / d.length).toFixed(1) : 0,
      tasaExito, eficienciaCobertura, convMasSolicitudes: topSolic?.titulo ?? '—',
      promedioDuracion, activas, vencidasSinCerrar, convSinSolicitudes: d.length - conSolic,
      convMasLarga: sorted[0] ?? { titulo: '—', dias: 0 },
      convMasCorta: sorted[sorted.length - 1] ?? { titulo: '—', dias: 0 },
      porMes, rankingSolicitudes: [...d].sort((a, b) => b.totalSolicitudes - a.totalSolicitudes).slice(0, 8)
        .map(c => ({ titulo: c.titulo, total: c.totalSolicitudes, estado: c.estadoConvocatoria })),
      duraciones: sorted.slice(0, 7), proximasACerrar,
    };
  }

  generarIA(): void {
    if (this.cargandoIA || !this.stats) return;
    this.cargandoIA = true; this.analisisIA = '';
    this.http.post<{ analisis: string }>(`${this.API}/revisor/reportes/analisis-ia`, { convocatorias: this.stats }).subscribe({
      next: r => { this.analisisIA = r.analisis ?? this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); },
      error: () => { this.analisisIA = this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); }
    });
  }
  private fallbackIA(): string {
    const s = this.stats!;
    return `El sistema registra ${s.total} convocatorias (${s.abiertas} abiertas, ${s.cerradas} cerradas, ${s.canceladas} canceladas). ` +
      `La eficiencia de cobertura es del ${s.eficienciaCobertura}% y la tasa de éxito en cerradas del ${s.tasaExito}%. ` +
      `Se cubrieron ${s.totalSolicitudesCubiertas} solicitudes con un promedio de ${s.promedioSolicitudesPorConv} por convocatoria. ` +
      (s.vencidasSinCerrar > 0 ? `⚠ ${s.vencidasSinCerrar} convocatoria(s) vencida(s) sin cerrar formalmente. ` : '') +
      (s.convSinSolicitudes > 0 ? `${s.convSinSolicitudes} convocatoria(s) sin solicitudes asignadas. ` : '') +
      (s.proximasACerrar.length > 0 ? `⚠ ${s.proximasACerrar.length} convocatoria(s) cierra(n) en los próximos 14 días.` : '');
  }

  abrirExport(): void { this.showExport = true; }
  cerrarExport(): void { if (!this.exportando) this.showExport = false; }
  exportar(): void {
    this.exportando = true;
    this.http.post(`${this.API}/admin/convocatorias/reporte/generar`, this.exportCfg, { responseType: 'blob', observe: 'response' }).subscribe({
      next: resp => {
        const ext = this.exportCfg.formato === 'EXCEL' ? 'xlsx' : 'pdf';
        const cd = resp.headers.get('content-disposition') ?? '';
        const nombre = (cd.match(/filename="?([^";\n]+)"?/) || [])[1] ?? `convocatorias.${ext}`;
        const url = window.URL.createObjectURL(resp.body!);
        const a = document.createElement('a'); a.href = url; a.download = nombre; a.click();
        window.URL.revokeObjectURL(url);
        this.exportando = false; this.cerrarExport();
        this.toast.success('Reporte generado', 'El archivo se descargó correctamente.');
        this.cdr.detectChanges();
      },
      error: () => { this.exportando = false; this.toast.error('Error', 'No se pudo generar el reporte.'); this.cdr.detectChanges(); }
    });
  }

  barWidth(v: number, max: number): string { return (!max ? 0 : Math.max(Math.round(v / max * 100), 2)) + '%'; }
  barH(v: number, max: number): string { return (!max ? 4 : Math.max(Math.round(v / max * 110), 4)) + 'px'; }
  maxMes(): number { return Math.max(...(this.stats?.porMes.map(m => m.count) ?? [1]), 1); }
  maxRank(): number { return Math.max(...(this.stats?.rankingSolicitudes.map(r => r.total) ?? [1]), 1); }
  maxDur(): number  { return Math.max(...(this.stats?.duraciones.map(d => d.dias) ?? [1]), 1); }
  diasRest(f: string): number { return Math.round((new Date(f).getTime() - Date.now()) / 86400000); }
  formatMes(m: string): string {
    const [y, mo] = m.split('-');
    return ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'][+mo-1] + " '" + y.slice(2);
  }
  gaugeOffset(pct: number): number { return 220 - 220 * Math.max(0, Math.min(100, pct)) / 100; }
  gaugeColor(pct: number): string { return pct >= 75 ? '#16a34a' : pct >= 45 ? '#d97706' : '#dc2626'; }
  colorEst(e: string): string { return ({ abierta:'#16a34a', cerrada:'#6b7280', cancelada:'#dc2626' } as any)[e] ?? '#6b7280'; }
  badgeClass(e: string): string { return ({ abierta:'b-ok', cerrada:'b-neutral', cancelada:'b-danger' } as any)[e] ?? ''; }
  colorIdx(i: number): string { return ['#00A63E','#2563eb','#d97706','#7c3aed','#0891b2','#dc2626','#059669','#be185d'][i % 8]; }
  volver(): void { this.router.navigate(['/revisor/convocatorias']); }
}
