import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { environment } from '../../../../environments/environment';

// ── Interfaces de datos ────────────────────────────────────────────────────

interface StatsConvocatoria {
  total: number;
  abiertas: number;
  cerradas: number;
  canceladas: number;
  porEstado: { estado: string; count: number; pct: number }[];
}

interface StatsPrepostulacion {
  total: number;
  aprobadas: number;
  rechazadas: number;
  pendientes: number;
  tasaAprobacion: number;
  porConvocatoria: { titulo: string; total: number; aprobadas: number; rechazadas: number }[];
}

interface StatsSolicitud {
  total: number;
  pendientes: number;
  aprobadas: number;
  rechazadas: number;
  docentesRequeridos: number;
  porFacultad: { facultad: string; count: number }[];
  porArea: { area: string; count: number }[];
}

// ── Interfaces de reporte ──────────────────────────────────────────────────

interface ReporteConfig {
  moduloActivo: 'convocatoria' | 'prepostulacion' | 'solicitud';
  titulo: string;
  subtitulo: string;
  formato: 'PDF' | 'EXCEL';
  orientacion: 'VERTICAL' | 'HORIZONTAL';
  colorPrimario: string;
  desde: string;
  hasta: string;
  estado: string;
  estadoRevision: string;
  incluirPortada: boolean;
  incluirKpis: boolean;
  incluirDetalle: boolean;
  incluirGraficoEstados: boolean;
  incluirGraficoTemporal: boolean;
  incluirGraficoCarreras: boolean;
  incluirGraficoAreas: boolean;
  incluirGraficoPrepostulaciones: boolean;
  incluirGraficoConvocatoria: boolean;
  mostrarNumeroPagina: boolean;
  mostrarFechaGeneracion: boolean;
  excelCongelarEncabezado: boolean;
  excelFiltrosAutomaticos: boolean;
  excelHojasPorSeccion: boolean;
  limite: number;
}

@Component({
  selector: 'app-reportes-revisor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ToastComponent],
  templateUrl: './reportes.html',
  styleUrls: ['./reportes.scss']
})
export class ReportesRevisorComponent implements OnInit {

  private readonly API = environment.apiUrl;

  // ── Estado de carga ──────────────────────────────────────────────────────
  cargandoConv    = false;
  cargandoPrepost = false;
  cargandoSolic   = false;
  cargandoIA      = false;

  // ── Datos ────────────────────────────────────────────────────────────────
  conv:    StatsConvocatoria    | null = null;
  prepost: StatsPrepostulacion  | null = null;
  solic:   StatsSolicitud       | null = null;
  analisisIA = '';

  // ── Filtros globales ─────────────────────────────────────────────────────
  filtros = { desde: '', hasta: '' };

  // ── Secciones colapsables ────────────────────────────────────────────────
  sec = {
    kpisGlobal:     true,
    convResumen:    true,
    convDetalle:    false,
    prepostResumen: true,
    prepostFunnel:  true,
    prepostPorConv: false,
    solicResumen:   true,
    solicFacultad:  true,
    solicArea:      false,
    ia:             true,
  };

  // ── Modal de reporte ─────────────────────────────────────────────────────
  modalAbierto    = false;
  generando       = false;
  tabReporte: 'formato' | 'secciones' | 'visual' = 'formato';

  cfg: ReporteConfig = {
    moduloActivo:   'prepostulacion',
    titulo:         'Reporte de Gestión Académica',
    subtitulo:      '',
    formato:        'PDF',
    orientacion:    'VERTICAL',
    colorPrimario:  '#00A63E',
    desde:          '',
    hasta:          '',
    estado:         '',
    estadoRevision: '',
    incluirPortada:              true,
    incluirKpis:                 true,
    incluirDetalle:              true,
    incluirGraficoEstados:       true,
    incluirGraficoTemporal:      true,
    incluirGraficoCarreras:      true,
    incluirGraficoAreas:         true,
    incluirGraficoPrepostulaciones: true,
    incluirGraficoConvocatoria:  true,
    mostrarNumeroPagina:         true,
    mostrarFechaGeneracion:      true,
    excelCongelarEncabezado:     true,
    excelFiltrosAutomaticos:     true,
    excelHojasPorSeccion:        true,
    limite:                      500,
  };

  constructor(
    private http:  HttpClient,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarTodo();
  }

  // ── Carga de datos ────────────────────────────────────────────────────────

  cargarTodo(): void {
    this.cargarConvocatorias();
    this.cargarPrepostulaciones();
    this.cargarSolicitudes();
  }

  cargarConvocatorias(): void {
    this.cargandoConv = true;
    this.http.get<any[]>(`${this.API}/admin/convocatorias`).subscribe({
      next: (data) => {
        const lista = Array.isArray(data) ? data : [];
        const abiertas  = lista.filter(c => c.estadoConvocatoria === 'abierta').length;
        const cerradas  = lista.filter(c => c.estadoConvocatoria === 'cerrada').length;
        const canceladas = lista.filter(c => c.estadoConvocatoria === 'cancelada').length;
        const total = lista.length;
        this.conv = {
          total, abiertas, cerradas, canceladas,
          porEstado: [
            { estado: 'Abiertas',   count: abiertas,  pct: total ? Math.round(abiertas  / total * 100) : 0 },
            { estado: 'Cerradas',   count: cerradas,  pct: total ? Math.round(cerradas  / total * 100) : 0 },
            { estado: 'Canceladas', count: canceladas, pct: total ? Math.round(canceladas / total * 100) : 0 },
          ]
        };
        this.cargandoConv = false;
        this.cdr.detectChanges();
      },
      error: () => { this.cargandoConv = false; this.cdr.detectChanges(); }
    });
  }

  cargarPrepostulaciones(): void {
    this.cargandoPrepost = true;
    this.http.get<any[]>(`${this.API}/admin/prepostulaciones`).subscribe({
      next: (data) => {
        const lista = Array.isArray(data) ? data : [];
        const aprobadas  = lista.filter(p => p.estadoRevision === 'APROBADO').length;
        const rechazadas = lista.filter(p => p.estadoRevision === 'RECHAZADO').length;
        const pendientes = lista.filter(p => p.estadoRevision === 'PENDIENTE').length;
        const total = lista.length;
        const tasaAprobacion = (aprobadas + rechazadas) > 0
          ? Math.round(aprobadas / (aprobadas + rechazadas) * 100)
          : 0;

        // Agrupar por convocatoria
        const byConv: Record<string, { titulo: string; total: number; aprobadas: number; rechazadas: number }> = {};
        lista.forEach(p => {
          const titulo = p.tituloConvocatoria || p.nombreConvocatoria || `Conv. ${p.idConvocatoria ?? 'S/N'}`;
          if (!byConv[titulo]) byConv[titulo] = { titulo, total: 0, aprobadas: 0, rechazadas: 0 };
          byConv[titulo].total++;
          if (p.estadoRevision === 'APROBADO')  byConv[titulo].aprobadas++;
          if (p.estadoRevision === 'RECHAZADO') byConv[titulo].rechazadas++;
        });

        this.prepost = {
          total, aprobadas, rechazadas, pendientes, tasaAprobacion,
          porConvocatoria: Object.values(byConv)
            .sort((a, b) => b.total - a.total)
            .slice(0, 8)
        };
        this.cargandoPrepost = false;
        this.cdr.detectChanges();
      },
      error: () => { this.cargandoPrepost = false; this.cdr.detectChanges(); }
    });
  }

  cargarSolicitudes(): void {
    this.cargandoSolic = true;
    this.http.get<any[]>(`${this.API}/solicitudes-docente`).subscribe({
      next: (data) => {
        const lista = Array.isArray(data) ? data : [];
        const pendientes = lista.filter(s => s.estadoSolicitud === 'pendiente').length;
        const aprobadas  = lista.filter(s => s.estadoSolicitud === 'aprobada').length;
        const rechazadas = lista.filter(s => s.estadoSolicitud === 'rechazada').length;
        const docentesRequeridos = lista.reduce((sum, s) => sum + (s.cantidadDocentes ?? 0), 0);

        // Por facultad
        const byFacultad: Record<string, number> = {};
        lista.forEach(s => {
          const f = s.nombreFacultad || 'Sin facultad';
          byFacultad[f] = (byFacultad[f] ?? 0) + 1;
        });

        // Por área
        const byArea: Record<string, number> = {};
        lista.forEach(s => {
          const a = s.nombreArea || 'Sin área';
          byArea[a] = (byArea[a] ?? 0) + 1;
        });

        this.solic = {
          total: lista.length, pendientes, aprobadas, rechazadas, docentesRequeridos,
          porFacultad: Object.entries(byFacultad)
            .map(([facultad, count]) => ({ facultad, count }))
            .sort((a, b) => b.count - a.count).slice(0, 8),
          porArea: Object.entries(byArea)
            .map(([area, count]) => ({ area, count }))
            .sort((a, b) => b.count - a.count).slice(0, 6),
        };
        this.cargandoSolic = false;
        this.cdr.detectChanges();
      },
      error: () => { this.cargandoSolic = false; this.cdr.detectChanges(); }
    });
  }

  // ── Análisis IA ───────────────────────────────────────────────────────────

  generarAnalisisIA(): void {
    if (this.cargandoIA) return;
    this.cargandoIA  = true;
    this.analisisIA  = '';

    const stats = {
      convocatorias:   this.conv,
      prepostulaciones: this.prepost,
      solicitudes:     this.solic,
    };

    this.http.post<{ analisis: string }>(
      `${this.API}/revisor/reportes/analisis-ia`, stats
    ).subscribe({
      next: (r) => {
        this.analisisIA = r.analisis ?? '';
        this.cargandoIA = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.analisisIA = this.generarAnalisisLocal();
        this.cargandoIA = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Fallback local si el backend no responde */
  private generarAnalisisLocal(): string {
    const c = this.conv;
    const p = this.prepost;
    const s = this.solic;
    if (!c || !p || !s) return 'No hay datos suficientes para generar un análisis.';

    const lines: string[] = [];
    lines.push(`Se registran ${c.total} convocatoria${c.total !== 1 ? 's' : ''} en total, `
      + `de las cuales ${c.abiertas} está${c.abiertas !== 1 ? 'n' : ''} actualmente abiertas.`);

    if (p.total > 0) {
      lines.push(`El proceso de selección acumula ${p.total} prepostulación${p.total !== 1 ? 'es' : ''} `
        + `con una tasa de aprobación del ${p.tasaAprobacion}%. `
        + `${p.pendientes} caso${p.pendientes !== 1 ? 's' : ''} permanece${p.pendientes === 1 ? '' : 'n'} pendiente${p.pendientes !== 1 ? 's' : ''} de revisión.`);
    }

    if (s.total > 0) {
      lines.push(`Se han generado ${s.total} solicitud${s.total !== 1 ? 'es' : ''} de docente `
        + `que representan una demanda de ${s.docentesRequeridos} docente${s.docentesRequeridos !== 1 ? 's' : ''}. `
        + `De estas, ${s.aprobadas} han sido aprobadas y ${s.pendientes} están en revisión.`);
    }

    if (p.tasaAprobacion < 50 && p.total > 10) {
      lines.push('La tasa de aprobación por debajo del 50% sugiere revisar los criterios de admisión '
        + 'o reforzar la orientación previa a los postulantes.');
    }
    if (c.abiertas === 0) {
      lines.push('No existen convocatorias actualmente abiertas. Se recomienda evaluar la apertura '
        + 'de nuevos procesos si hay solicitudes pendientes de cubrir.');
    }

    return lines.join(' ');
  }

  // ── Filtros ───────────────────────────────────────────────────────────────

  limpiarFiltros(): void {
    this.filtros = { desde: '', hasta: '' };
    this.cargarTodo();
  }

  toggle(key: keyof typeof this.sec): void {
    this.sec[key] = !this.sec[key];
  }

  // ── Utilidades de gráficos ────────────────────────────────────────────────

  barWidth(val: number, max: number): string {
    if (!max) return '0%';
    return Math.max(Math.round((val / max) * 100), 2) + '%';
  }

  // Ángulo SVG para el donut (radio 28, circunferencia ≈ 175.9)
  donutOffset(pct: number): number {
    const circ = 175.9;
    return circ - (circ * Math.max(0, Math.min(100, pct)) / 100);
  }

  maxConv(): number {
    return Math.max(...(this.conv?.porEstado.map(e => e.count) ?? [1]), 1);
  }

  maxPrepostConv(): number {
    return Math.max(...(this.prepost?.porConvocatoria.map(e => e.total) ?? [1]), 1);
  }

  maxFacultad(): number {
    return Math.max(...(this.solic?.porFacultad.map(f => f.count) ?? [1]), 1);
  }

  maxArea(): number {
    return Math.max(...(this.solic?.porArea.map(a => a.count) ?? [1]), 1);
  }

  colorEstadoConv(estado: string): string {
    return ({ Abiertas: '#16a34a', Cerradas: '#9ca3af', Canceladas: '#dc2626' } as any)[estado] ?? '#6b7280';
  }

  colorFacultad(i: number): string {
    const cols = ['#00A63E','#2563eb','#d97706','#7c3aed','#0891b2','#dc2626','#059669','#be185d'];
    return cols[i % cols.length];
  }

  estadoConvClass(estado: string): string {
    return ({ abierta: 'badge-success', cerrada: 'badge-neutral', cancelada: 'badge-danger' } as any)[estado] ?? '';
  }

  get cargandoTodo(): boolean {
    return this.cargandoConv || this.cargandoPrepost || this.cargandoSolic;
  }

  // ── Modal de reporte ──────────────────────────────────────────────────────

  abrirModal(): void {
    this.cfg.desde = this.filtros.desde;
    this.cfg.hasta = this.filtros.hasta;
    this.tabReporte = 'formato';
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    if (this.generando) return;
    this.modalAbierto = false;
  }

  get endpointReporte(): string {
    const map: Record<string, string> = {
      convocatoria:   `${this.API}/admin/convocatorias/reporte/generar`,
      prepostulacion: `${this.API}/admin/prepostulaciones/reporte/generar`,
      solicitud:      `${this.API}/admin/solicitudes-docentes/reporte/generar`,
    };
    return map[this.cfg.moduloActivo] ?? map['prepostulacion'];
  }

  get bodyReporte(): Record<string, any> {
    const base = {
      titulo:            this.cfg.titulo,
      subtitulo:         this.cfg.subtitulo,
      formato:           this.cfg.formato,
      orientacion:       this.cfg.orientacion,
      desde:             this.cfg.desde,
      hasta:             this.cfg.hasta,
      colorPrimario:     this.cfg.colorPrimario,
      incluirPortada:    this.cfg.incluirPortada,
      incluirKpis:       this.cfg.incluirKpis,
      incluirDetalle:    this.cfg.incluirDetalle,
      mostrarNumeroPagina:    this.cfg.mostrarNumeroPagina,
      mostrarFechaGeneracion: this.cfg.mostrarFechaGeneracion,
      excelCongelarEncabezado:  this.cfg.excelCongelarEncabezado,
      excelFiltrosAutomaticos:  this.cfg.excelFiltrosAutomaticos,
      excelHojasPorSeccion:     this.cfg.excelHojasPorSeccion,
      limite:            this.cfg.limite,
    };
    if (this.cfg.moduloActivo === 'convocatoria') {
      return { ...base, estado: this.cfg.estado,
        incluirGraficoEstados: this.cfg.incluirGraficoEstados,
        incluirGraficoPrepostulaciones: this.cfg.incluirGraficoPrepostulaciones,
        incluirGraficoTemporal: this.cfg.incluirGraficoTemporal };
    }
    if (this.cfg.moduloActivo === 'prepostulacion') {
      return { ...base, estadoRevision: this.cfg.estadoRevision,
        incluirGraficoEstados: this.cfg.incluirGraficoEstados,
        incluirGraficoTemporal: this.cfg.incluirGraficoTemporal,
        incluirGraficoConvocatoria: this.cfg.incluirGraficoConvocatoria };
    }
    return { ...base, estado: this.cfg.estado,
      incluirGraficoEstados: this.cfg.incluirGraficoEstados,
      incluirGraficoCarreras: this.cfg.incluirGraficoCarreras,
      incluirGraficoAreas: this.cfg.incluirGraficoAreas,
      incluirGraficoTemporal: this.cfg.incluirGraficoTemporal };
  }

  generarReporte(): void {
    this.generando = true;
    this.http.post(this.endpointReporte, this.bodyReporte, {
      responseType: 'blob', observe: 'response'
    }).subscribe({
      next: (response) => {
        const blob = response.body!;
        const cd   = response.headers.get('content-disposition') ?? '';
        const match = cd.match(/filename="?([^";\n]+)"?/);
        const ext  = this.cfg.formato === 'EXCEL' ? 'xlsx' : 'pdf';
        const nombre = match ? match[1] : `reporte_${this.cfg.moduloActivo}.${ext}`;
        const url = window.URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href = url; a.download = nombre; a.click();
        window.URL.revokeObjectURL(url);
        this.generando = false;
        this.cerrarModal();
        this.toast.success('Reporte generado', 'El archivo se descargó correctamente.');
        this.cdr.detectChanges();
      },
      error: () => {
        this.generando = false;
        this.toast.error('Error', 'No se pudo generar el reporte. Verifica la conexión con el servidor.');
        this.cdr.detectChanges();
      }
    });
  }

  get moduloLabel(): string {
    return ({ convocatoria: 'Convocatorias', prepostulacion: 'Prepostulaciones', solicitud: 'Solicitudes de Docente' } as any)[this.cfg.moduloActivo];
  }
}
