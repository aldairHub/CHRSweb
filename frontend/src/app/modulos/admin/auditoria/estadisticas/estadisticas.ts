import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, NgSwitch, NgSwitchCase } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ToastComponent } from '../../../../component/toast.component';
import { ToastService } from '../../../../services/toast.service';
import { ReporteAuditoriaService, ReporteAuditoriaConfig } from '../../../../services/reporte-auditoria.service';

// ── Interfaces ───────────────────────────────────────────────────────────────

interface StatsLogin {
  totalRegistros: number;
  totalExitosos:  number;
  totalFallidos:  number;
  tasaExito:      number;
  tendenciaDiaria: { dia: string; total: number; exitosos: number; fallidos: number }[];
  topFallidos:    { usuario: string; intentos: number }[];
  topExitosos:    { usuario: string; accesos: number }[];
  porHora:        { hora: number; total: number }[];
}

interface StatsCambios {
  totalCambios:    number;
  totalInsert:     number;
  totalUpdate:     number;
  totalDelete:     number;
  porTabla:        { tabla: string; cambios: number }[];
  tendenciaDiaria: { dia: string; total: number; inserts: number; updates: number; deletes: number }[];
  topUsuarios:     { usuario: string; usuarioBd: string; cambios: number }[];
  camposFrecuentes:{ campo: string; tabla: string; veces: number }[];
  cambiosExternos: { tabla: string; cambios: number }[];
}

@Component({
  selector: 'app-estadisticas-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, ToastComponent, NgSwitch, NgSwitchCase],
  templateUrl: './estadisticas.html',
  styleUrls: ['./estadisticas.scss']
})
export class EstadisticasAuditoriaComponent implements OnInit {

  private readonly api = 'http://localhost:8080/api/admin/auditoria';

  // ── Filtros globales ─────────────────────────────────────────────────────
  filtros = {
    desde:      '',
    hasta:      '',
    usuarioApp: '',
    resultado:  '',   // solo login
    tabla:      '',   // solo cambios
    operacion:  '',   // solo cambios
  };
  modalReporteAbierto = false;
  generandoReporte    = false;
  cfgReporte!: ReporteAuditoriaConfig;
  // ── Estado de carga ──────────────────────────────────────────────────────
  loadingLogin    = false;
  loadingCambios  = false;

  // ── Datos ────────────────────────────────────────────────────────────────
  login:   StatsLogin   | null = null;
  cambios: StatsCambios | null = null;

  // ── Secciones colapsables ────────────────────────────────────────────────
  secciones = {
    kpisLogin:      true,
    tendenciaLogin: true,
    topUsuarios:    true,
    porHora:        false,
    kpisCambios:    true,
    tendenciaCambios: true,
    porTabla:       true,
    camposFrecuentes: false,
    externos:       false,
  };

  // ── Control de gráficos ──────────────────────────────────────────────────
  metricaLogin:   'todos' | 'exitosos' | 'fallidos' = 'todos';
  metricaCambios: 'total' | 'inserts'  | 'updates' | 'deletes' = 'total';

  constructor(
    private http:  HttpClient,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService,
    private reporteSvc: ReporteAuditoriaService
  ) {}

  ngOnInit(): void {
    this.cargarTodo();
  }

  // ── Carga ────────────────────────────────────────────────────────────────

  cargarTodo(): void {
    this.cargarLogin();
    this.cargarCambios();
  }

  cargarLogin(): void {
    this.loadingLogin = true;
    let params = this.buildParams(['desde', 'hasta', 'usuarioApp', 'resultado']);

    this.http.get<any>(`${this.api}/estadisticas/login`, { params }).subscribe({
      next: (data) => {
        this.login = this.mapLogin(data);
        this.loadingLogin = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingLogin = false;
        this.toast.error('Error', 'No se pudieron cargar las estadísticas de accesos.');
        this.cdr.detectChanges();
      }
    });
  }

  cargarCambios(): void {
    this.loadingCambios = true;
    let params = this.buildParams(['desde', 'hasta', 'tabla', 'operacion', 'usuarioApp']);

    this.http.get<any>(`${this.api}/estadisticas/cambios`, { params }).subscribe({
      next: (data) => {
        this.cambios = this.mapCambios(data);
        this.loadingCambios = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingCambios = false;
        this.toast.error('Error', 'No se pudieron cargar las estadísticas de cambios.');
        this.cdr.detectChanges();
      }
    });
  }

  limpiarFiltros(): void {
    this.filtros = { desde: '', hasta: '', usuarioApp: '', resultado: '', tabla: '', operacion: '' };
    this.cargarTodo();
  }

  // ── Mapeo de respuesta del backend ────────────────────────────────────────

  private mapLogin(d: any): StatsLogin {
    const tendencia = this.parseJson(d.tendenciaDiaria).map((r: any) => ({
      dia:      String(r.dia ?? ''),
      total:    Number(r.total    ?? 0),
      exitosos: Number(r.exitosos ?? 0),
      fallidos: Number(r.fallidos ?? 0),
    }));
    const topFallidos = this.parseJson(d.topFallidos).map((r: any) => ({
      usuario: String(r.usuario ?? ''),
      intentos: Number(r.intentos ?? 0),
    }));
    const topExitosos = this.parseJson(d.topExitosos).map((r: any) => ({
      usuario: String(r.usuario ?? ''),
      accesos: Number(r.accesos ?? 0),
    }));
    const porHora = this.parseJson(d.porHora).map((r: any) => ({
      hora:  Number(r.hora  ?? 0),
      total: Number(r.total ?? 0),
    }));
    return {
      totalRegistros:  Number(d.totalRegistros  ?? 0),
      totalExitosos:   Number(d.totalExitosos   ?? 0),
      totalFallidos:   Number(d.totalFallidos   ?? 0),
      tasaExito:       Number(d.tasaExito       ?? 0),
      tendenciaDiaria: tendencia,
      topFallidos,
      topExitosos,
      porHora,
    };
  }

  private mapCambios(d: any): StatsCambios {
    const tendencia = this.parseJson(d.tendenciaDiaria).map((r: any) => ({
      dia:     String(r.dia     ?? ''),
      total:   Number(r.total   ?? 0),
      inserts: Number(r.inserts ?? 0),
      updates: Number(r.updates ?? 0),
      deletes: Number(r.deletes ?? 0),
    }));
    const porTabla = this.parseJson(d.porTabla).map((r: any) => ({
      tabla:   String(r.tabla   ?? ''),
      cambios: Number(r.cambios ?? 0),
    }));
    const topUsuarios = this.parseJson(d.topUsuarios).map((r: any) => ({
      usuario:   String(r.usuario   ?? ''),
      usuarioBd: String(r.usuarioBd ?? ''),
      cambios:   Number(r.cambios   ?? 0),
    }));
    const camposFrecuentes = this.parseJson(d.camposFrecuentes).map((r: any) => ({
      campo: String(r.campo ?? ''),
      tabla: String(r.tabla ?? ''),
      veces: Number(r.veces ?? 0),
    }));
    const cambiosExternos = this.parseJson(d.cambiosExternos).map((r: any) => ({
      tabla:   String(r.tabla   ?? ''),
      cambios: Number(r.cambios ?? 0),
    }));
    return {
      totalCambios:    Number(d.totalCambios ?? 0),
      totalInsert:     Number(d.totalInsert  ?? 0),
      totalUpdate:     Number(d.totalUpdate  ?? 0),
      totalDelete:     Number(d.totalDelete  ?? 0),
      porTabla,
      tendenciaDiaria: tendencia,
      topUsuarios,
      camposFrecuentes,
      cambiosExternos,
    };
  }

  private parseJson(val: any): any[] {
    if (!val) return [];
    if (Array.isArray(val)) return val;
    if (typeof val === 'string') {
      try { return JSON.parse(val); } catch { return []; }
    }
    return [];
  }

  // ── Utilidades de gráficos ────────────────────────────────────────────────

  buildParams(keys: string[]): HttpParams {
    let params = new HttpParams();
    keys.forEach(k => {
      const val = (this.filtros as any)[k];
      if (val && val.trim()) params = params.set(k, val.trim());
    });
    return params;
  }

  barHeight(val: number, max: number): string {
    if (!max) return '4px';
    return Math.max(Math.round((val / max) * 130), 4) + 'px';
  }

  barWidth(val: number, max: number): string {
    if (!max) return '0%';
    return Math.round((val / max) * 100) + '%';
  }

  maxLogin(): number {
    if (!this.login?.tendenciaDiaria?.length) return 1;
    const field = this.metricaLogin === 'exitosos' ? 'exitosos'
      : this.metricaLogin === 'fallidos' ? 'fallidos' : 'total';
    return Math.max(...this.login.tendenciaDiaria.map((d: any) => d[field]), 1);
  }

  maxCambios(): number {
    if (!this.cambios?.tendenciaDiaria?.length) return 1;
    const field = this.metricaCambios === 'inserts' ? 'inserts'
      : this.metricaCambios === 'updates' ? 'updates'
        : this.metricaCambios === 'deletes' ? 'deletes' : 'total';
    return Math.max(...this.cambios.tendenciaDiaria.map((d: any) => d[field]), 1);
  }

  maxPorTabla(): number {
    if (!this.cambios?.porTabla?.length) return 1;
    return Math.max(...this.cambios.porTabla.map(t => t.cambios), 1);
  }

  maxFallidos(): number {
    if (!this.login?.topFallidos?.length) return 1;
    return Math.max(...this.login.topFallidos.map(u => u.intentos), 1);
  }

  maxExitosos(): number {
    if (!this.login?.topExitosos?.length) return 1;
    return Math.max(...this.login.topExitosos.map(u => u.accesos), 1);
  }

  maxHora(): number {
    if (!this.login?.porHora?.length) return 1;
    return Math.max(...this.login.porHora.map(h => h.total), 1);
  }

  formatFecha(fecha: string): string {
    return new Date(fecha + 'T00:00:00').toLocaleDateString('es-EC', {
      weekday: 'short', day: '2-digit', month: 'short'
    });
  }

  formatHora(hora: number): string {
    return hora.toString().padStart(2, '0') + ':00';
  }

  toggle(key: keyof typeof this.secciones): void {
    this.secciones[key] = !this.secciones[key];
  }

  tablaColor(tabla: string): string {
    const colores: Record<string, string> = {
      usuario: '#6366f1', convocatoria: '#0891b2', prepostulacion: '#d97706',
      postulacion: '#059669', solicitud_docente: '#dc2626', evaluacion: '#7c3aed',
      proceso_evaluacion: '#be185d', documento: '#b45309'
    };
    return colores[tabla] ?? '#6b7280';
  }

  abrirReporte(): void {
    // Pre-carga los filtros actuales del dashboard en la config del reporte
    this.cfgReporte = this.reporteSvc.configDefecto({
      desde:      this.filtros.desde,
      hasta:      this.filtros.hasta,
      usuarioApp: this.filtros.usuarioApp,
      resultado:  this.filtros.resultado,
      tabla:      this.filtros.tabla,
      operacion:  this.filtros.operacion,
    });
    this.modalReporteAbierto = true;
  }

  cerrarReporte(): void {
    if (this.generandoReporte) return;
    this.modalReporteAbierto = false;
  }

  generarReporte(): void {
    this.generandoReporte = true;
    this.reporteSvc.generar(this.cfgReporte).subscribe({
      next: (blob) => {
        this.reporteSvc.descargar(blob, this.cfgReporte);
        this.generandoReporte    = false;
        this.modalReporteAbierto = false;
        this.toast.success('Reporte generado', 'El archivo se descargó correctamente.');
        this.cdr.detectChanges();
      },
      error: () => {
        this.generandoReporte = false;
        this.toast.error('Error', 'No se pudo generar el reporte.');
        this.cdr.detectChanges();
      }
    });
  }
}
