import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { environment } from '../../../../environments/environment';

interface PostulanteItem {
  idPrepostulacion: number; nombres: string; apellidos: string;
  identificacion: string; correo: string; estadoRevision: string;
  fechaEnvio: string; idConvocatoria: number | null; idSolicitud: number | null;
}

interface Stats {
  total: number; aprobados: number; rechazados: number; pendientes: number;
  tasaAprobacion: number; tasaRechazo: number; tiempoPromedioRevision: number;
  revisados: number; sinRevisar: number;
  porDia: { dia: string; total: number; aprobados: number; rechazados: number }[];
  porConvocatoria: { id: number | null; count: number; aprobados: number; rechazados: number }[];
  ultimosIngresados: PostulanteItem[];
  horasPromedioPendientes: number;
  distribucion: { label: string; val: number; pct: number; color: string }[];
}

// ── Interfaces para ranking IA ──────────────────────────────────────────────
interface SolicitudItem {
  idSolicitud: number;
  nivelAcademico: string;
  estadoSolicitud: string;
  materia?: string;
}

interface RankingResultado {
  idPrepostulacion: number;
  nombre: string;
  correo: string;
  identificacion: string;
  estadoRevision: string;
  posicion: number;
  puntuacion: number;
  nivelCumplimiento: 'ALTO' | 'MEDIO' | 'BAJO';
  resumen: string;
  fortalezas: string[];
  observaciones: string;
}

interface RankingResponse {
  ranking: RankingResultado[];
  resumenGeneral: string;
  mensaje?: string;
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

  // ── Estado existente (sin cambios) ─────────────────────────────────────────
  cargando = false; cargandoIA = false; analisisIA = ''; analisisSecciones: { titulo: string; contenido: string; esAlerta: boolean; esRec: boolean }[] = [];
  datos: PostulanteItem[] = []; stats: Stats | null = null; ultimaAct = '';
  metricaTend: 'total' | 'aprobados' | 'rechazados' = 'total';

  showExport = false; exportando = false;
  exportCfg = {
    formato: 'PDF' as 'PDF'|'EXCEL', orientacion: 'HORIZONTAL' as 'VERTICAL'|'HORIZONTAL',
    titulo: 'Estadísticas de Prepostulaciones', subtitulo: '', colorPrimario: '#00A63E',
    estadoRevision: '', desde: '', hasta: '', idsConvocatoria: [] as number[], idsSolicitud: [] as number[],
    limite: 500, incluirPortada: true, incluirKpis: true, incluirDetalle: true,
    incluirGraficoEstados: true, incluirGraficoTemporal: true, incluirGraficoConvocatoria: true,
    tipoGrafico: 'BAR', mostrarNumeroPagina: true, mostrarFechaGeneracion: true,
    excelCongelarEncabezado: true, excelFiltrosAutomaticos: true, excelHojasPorSeccion: true,
  };

  // ── Nuevo: modal IA con selector de modo ───────────────────────────────────
  showModalIA       = false;
  modoIA: 'elegir' | 'general' | 'ranking' = 'elegir';

  // Modo ranking
  solicitudes: SolicitudItem[]    = [];
  cargandoSolicitudes             = false;
  idSolicitudRanking: number | null = null;
  rankingAnalizarDocs             = true;
  rankingAnalizarNivel            = true;
  cargandoRanking                 = false;
  rankingResultados: RankingResultado[] = [];
  rankingResumenGeneral           = '';
  rankingError                    = '';
  rankingListo                    = false;

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef,
              private router: Router, private toast: ToastService) {}

  ngOnInit(): void { this.cargar(); }

  // ── Carga de datos (sin cambios) ───────────────────────────────────────────
  cargar(): void {
    this.cargando = true;
    this.http.get<PostulanteItem[]>(`${this.API}/admin/prepostulaciones`).subscribe({
      next: d => {
        this.datos = Array.isArray(d) ? d : [];
        this.calcular();
        this.cargando = false;
        this.ultimaAct = new Date().toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' });
        this.cdr.detectChanges();
      },
      error: () => { this.cargando = false; this.toast.error('Error', 'No se pudieron cargar las prepostulaciones.'); this.cdr.detectChanges(); }
    });
  }

  private calcular(): void {
    const d = this.datos;
    if (!d.length) { this.stats = null; return; }
    const aprobados  = d.filter(p => p.estadoRevision === 'APROBADO').length;
    const rechazados = d.filter(p => p.estadoRevision === 'RECHAZADO').length;
    const pendientes = d.filter(p => p.estadoRevision === 'PENDIENTE').length;
    const revisados  = aprobados + rechazados;
    const tasaAprobacion = revisados > 0 ? Math.round(aprobados  / revisados * 100) : 0;
    const tasaRechazo    = revisados > 0 ? Math.round(rechazados / revisados * 100) : 0;
    const ahora = Date.now();
    const tiempos = d.filter(p => p.fechaEnvio && p.estadoRevision !== 'PENDIENTE')
      .map(p => (ahora - new Date(p.fechaEnvio).getTime()) / 86400000);
    const tiempoPromedioRevision = tiempos.length ? +(tiempos.reduce((s, t) => s + t, 0) / tiempos.length).toFixed(1) : 0;
    const pendList = d.filter(p => p.estadoRevision === 'PENDIENTE' && p.fechaEnvio);
    const horasPromedioPendientes = pendList.length
      ? Math.round(pendList.reduce((s, p) => s + (ahora - new Date(p.fechaEnvio).getTime()) / 3600000, 0) / pendList.length)
      : 0;
    const byDia: Record<string, { total: number; aprobados: number; rechazados: number }> = {};
    d.forEach(p => {
      if (!p.fechaEnvio) return;
      const dia = p.fechaEnvio.slice(0, 10);
      if (!byDia[dia]) byDia[dia] = { total: 0, aprobados: 0, rechazados: 0 };
      byDia[dia].total++;
      if (p.estadoRevision === 'APROBADO')  byDia[dia].aprobados++;
      if (p.estadoRevision === 'RECHAZADO') byDia[dia].rechazados++;
    });
    const porDia = Object.entries(byDia).sort(([a], [b]) => a.localeCompare(b)).slice(-14)
      .map(([dia, v]) => ({ dia, ...v }));
    const byConv: Record<string, { id: number|null; count: number; aprobados: number; rechazados: number }> = {};
    d.forEach(p => {
      const key = String(p.idConvocatoria ?? 'sin');
      if (!byConv[key]) byConv[key] = { id: p.idConvocatoria, count: 0, aprobados: 0, rechazados: 0 };
      byConv[key].count++;
      if (p.estadoRevision === 'APROBADO')  byConv[key].aprobados++;
      if (p.estadoRevision === 'RECHAZADO') byConv[key].rechazados++;
    });
    const porConvocatoria = Object.values(byConv).sort((a, b) => b.count - a.count).slice(0, 8);
    const distribucion = [
      { label: 'Aprobados',  val: aprobados,  pct: d.length ? Math.round(aprobados  / d.length * 100) : 0, color: '#16a34a' },
      { label: 'Rechazados', val: rechazados, pct: d.length ? Math.round(rechazados / d.length * 100) : 0, color: '#dc2626' },
      { label: 'Pendientes', val: pendientes, pct: d.length ? Math.round(pendientes / d.length * 100) : 0, color: '#d97706' },
    ];
    this.stats = {
      total: d.length, aprobados, rechazados, pendientes, revisados,
      sinRevisar: pendientes, tasaAprobacion, tasaRechazo,
      tiempoPromedioRevision, horasPromedioPendientes,
      porDia, porConvocatoria, distribucion,
      ultimosIngresados: [...d].filter(p => p.fechaEnvio)
        .sort((a, b) => b.fechaEnvio.localeCompare(a.fechaEnvio)).slice(0, 6),
    };
  }

  // ── Análisis general (sin cambios internos) ────────────────────────────────
  generarIA(): void {
    if (this.cargandoIA || !this.stats) return;
    this.cargandoIA = true; this.analisisIA = ''; this.analisisSecciones = [];
    this.http.post<{ analisis: string }>(`${this.API}/revisor/reportes/analisis-ia`, { prepostulaciones: this.stats }).subscribe({
      next: r => {
        this.analisisIA = r.analisis ?? this.fallbackIA();
        this.analisisSecciones = this.parsearSecciones(this.analisisIA);
        this.cargandoIA = false; this.cdr.detectChanges();
      },
      error: () => { this.analisisIA = this.fallbackIA(); this.analisisSecciones = this.parsearSecciones(this.analisisIA); this.cargandoIA = false; this.cdr.detectChanges(); }
    });
  }

  parsearSecciones(texto: string): { titulo: string; contenido: string; esAlerta: boolean; esRec: boolean }[] {
    if (!texto || !texto.trim()) return [];
    const patron = /^\s*\*{0,2}(SITUACI[OÓ]N ACTUAL|AN[AÁ]LISIS(?: DE RENDIMIENTO)?|ALERTAS?(?: Y RIESGOS?)?|RECOMENDACIONES?)\*{0,2}\s*:/gim;
    const titulos: string[] = [];
    const posiciones: number[] = [];
    let m: RegExpExecArray | null;
    while ((m = patron.exec(texto)) !== null) {
      titulos.push(m[1].trim());
      posiciones.push(m.index + m[0].length);
    }
    if (titulos.length === 0) return [];
    return titulos.map((titulo, i) => {
      const inicio  = posiciones[i];
      const fin     = i + 1 < posiciones.length ? (texto.lastIndexOf('\n', posiciones[i + 1] - titulos[i + 1].length - 5) || posiciones[i + 1]) : texto.length;
      const contenido = texto.slice(inicio, fin).trim();
      return { titulo, contenido, esAlerta: titulo.toUpperCase().includes('ALERTA'), esRec: titulo.toUpperCase().includes('RECOMEND') };
    });
  }

  private fallbackIA(): string {
    const s = this.stats!;
    const pctRev = s.total > 0 ? Math.round(s.revisados / s.total * 100) : 0;
    let r = '';
    r += `SITUACIÓN ACTUAL: Se registran ${s.total} prepostulación${s.total !== 1 ? 'es' : ''} en total. De estas, ${s.revisados} han sido revisadas (${pctRev}%): ${s.aprobados} aprobadas y ${s.rechazados} rechazadas. Quedan ${s.pendientes} pendientes de revisión.\n\n`;
    r += `ANÁLISIS DE RENDIMIENTO: La tasa de aprobación sobre revisadas es del ${s.tasaAprobacion}% y la tasa de rechazo del ${s.tasaRechazo}%.`;
    if (s.tiempoPromedioRevision > 0) r += ` El tiempo promedio de revisión es de ${s.tiempoPromedioRevision} días.`;
    if (s.horasPromedioPendientes > 0) r += ` Los pendientes llevan en espera un promedio de ${s.horasPromedioPendientes} horas.`;
    r += '\n\n';
    let alertas = '';
    if (s.tasaAprobacion < 50 && s.revisados > 5) alertas += `Tasa de aprobación inferior al 50% — revisar criterios de admisión. `;
    if (s.pendientes > s.revisados) alertas += `El volumen pendiente (${s.pendientes}) supera al revisado — riesgo de retraso en el proceso. `;
    r += `ALERTAS Y RIESGOS: ${alertas || 'No se detectan alertas críticas en este momento.'}\n\n`;
    let rec = '';
    if (s.pendientes > 5) rec += `Priorizar la revisión de las ${s.pendientes} prepostulaciones pendientes. `;
    if (s.tasaAprobacion < 50 && s.revisados > 5) rec += 'Revisar y ajustar los criterios de evaluación docente. ';
    if (!rec) rec = 'Mantener el ritmo de revisión actual para no generar retrasos.';
    r += `RECOMENDACIONES: ${rec}`;
    return r.trim();
  }

  // ── NUEVO: modal IA ────────────────────────────────────────────────────────

  abrirModalIA(): void {
    this.modoIA = 'elegir';
    this.rankingListo = false;
    this.rankingResultados = [];
    this.rankingResumenGeneral = '';
    this.rankingError = '';
    this.idSolicitudRanking = null;
    this.showModalIA = true;

    // Cargar solicitudes si no están aún
    if (!this.solicitudes.length) {
      this.cargarSolicitudes();
    }
  }

  cerrarModalIA(): void {
    if (this.cargandoIA || this.cargandoRanking) return;
    this.showModalIA = false;
  }

  elegirModo(modo: 'general' | 'ranking'): void {
    this.modoIA = modo;
    if (modo === 'general') {
      this.analisisIA = '';
      this.analisisSecciones = [];
      this.generarIA();
    }
  }

  volverAElegir(): void {
    this.modoIA = 'elegir';
    this.rankingListo = false;
    this.rankingResultados = [];
    this.rankingResumenGeneral = '';
    this.rankingError = '';
  }

  private cargarSolicitudes(): void {
    this.cargandoSolicitudes = true;
    this.http.get<any[]>(`${this.API}/solicitudes-docente`).subscribe({
      next: data => {
        this.solicitudes = (data || []).map(s => ({
          idSolicitud:    s.idSolicitud,
          nivelAcademico: s.nivelAcademico,
          estadoSolicitud: s.estadoSolicitud,
          materia:        s.materia?.nombreMateria ?? s.nombreMateria ?? ''
        }));
        this.cargandoSolicitudes = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoSolicitudes = false;
        this.cdr.detectChanges();
      }
    });
  }

  generarRanking(): void {
    if (!this.idSolicitudRanking) {
      this.rankingError = 'Selecciona una solicitud para continuar.';
      return;
    }
    if (!this.rankingAnalizarDocs && !this.rankingAnalizarNivel) {
      this.rankingError = 'Selecciona al menos un criterio de análisis.';
      return;
    }

    this.rankingError = '';
    this.cargandoRanking = true;
    this.rankingListo = false;
    this.rankingResultados = [];

    this.http.post<RankingResponse>(`${this.API}/admin/prepostulaciones/ia/ranking`, {
      idSolicitud:           this.idSolicitudRanking,
      analizarDocumentos:    this.rankingAnalizarDocs,
      analizarNivelAcademico: this.rankingAnalizarNivel
    }).subscribe({
      next: res => {
        this.cargandoRanking = false;
        this.rankingResultados    = res.ranking || [];
        this.rankingResumenGeneral = res.resumenGeneral || '';
        this.rankingListo = true;

        if (!this.rankingResultados.length) {
          this.rankingError = res.mensaje || 'No se encontraron pre-postulantes para esta solicitud.';
        }
        this.cdr.detectChanges();
      },
      error: err => {
        this.cargandoRanking = false;
        this.rankingError = err?.error?.error || 'Error al contactar Mistral AI. Intenta de nuevo.';
        this.cdr.detectChanges();
      }
    });
  }

  // ── Helpers ranking ────────────────────────────────────────────────────────
  nivelClass(n: string): string {
    return ({ ALTO: 'rnk-alto', MEDIO: 'rnk-medio', BAJO: 'rnk-bajo' } as any)[n] ?? '';
  }
  puntuacionColor(p: number): string {
    return p >= 75 ? '#16a34a' : p >= 50 ? '#d97706' : '#dc2626';
  }
  posicionEmoji(p: number): string {
    return p === 1 ? '🥇' : p === 2 ? '🥈' : p === 3 ? '🥉' : `#${p}`;
  }
  solicitudLabel(s: SolicitudItem): string {
    return `#${s.idSolicitud} · ${s.nivelAcademico}${s.materia ? ' — ' + s.materia : ''}`;
  }
  get puedeGenerarRanking(): boolean {
    return !!this.idSolicitudRanking && (this.rankingAnalizarDocs || this.rankingAnalizarNivel);
  }

  // ── Exportar (sin cambios) ─────────────────────────────────────────────────
  abrirExport(): void { this.showExport = true; }
  cerrarExport(): void { if (!this.exportando) this.showExport = false; }
  exportar(): void {
    this.exportando = true;
    this.http.post(`${this.API}/admin/prepostulaciones/reporte/generar`, this.exportCfg, { responseType: 'blob', observe: 'response' }).subscribe({
      next: resp => {
        const ext = this.exportCfg.formato === 'EXCEL' ? 'xlsx' : 'pdf';
        const cd = resp.headers.get('content-disposition') ?? '';
        const nombre = (cd.match(/filename="?([^";\n]+)"?/) || [])[1] ?? `prepostulaciones.${ext}`;
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

  // ── Helpers gráficos (sin cambios) ────────────────────────────────────────
  get maxTend(): number { return Math.max(...(this.stats?.porDia.map(d => (d as any)[this.metricaTend]) ?? [1]), 1); }
  tendVal(d: any): number { return (d as any)[this.metricaTend]; }
  colorTend(): string { return ({ total:'#2563eb', aprobados:'#16a34a', rechazados:'#dc2626' } as any)[this.metricaTend]; }
  maxConv(): number { return Math.max(...(this.stats?.porConvocatoria.map(c => c.count) ?? [1]), 1); }
  barWidth(v: number, max: number): string { return (!max ? 0 : Math.max(Math.round(v / max * 100), 2)) + '%'; }
  barH(v: number, max: number): string { return (!max ? 4 : Math.max(Math.round(v / max * 110), 4)) + 'px'; }
  gaugeOffset(pct: number): number { return 220 - 220 * Math.max(0, Math.min(100, pct)) / 100; }
  gaugeColor(pct: number): string { return pct >= 75 ? '#16a34a' : pct >= 45 ? '#d97706' : '#dc2626'; }
  formatFecha(f: string): string { return !f ? '' : new Date(f + 'T00:00:00').toLocaleDateString('es-EC', { day: '2-digit', month: 'short' }); }
  labelEst(e: string): string { return ({ APROBADO:'Aprobado', RECHAZADO:'Rechazado', PENDIENTE:'Pendiente' } as any)[e] ?? e; }
  badgeClass(e: string): string { return ({ APROBADO:'b-ok', RECHAZADO:'b-danger', PENDIENTE:'b-warn' } as any)[e] ?? ''; }
  volver(): void { this.router.navigate(['/revisor/prepostulaciones']); }
}
