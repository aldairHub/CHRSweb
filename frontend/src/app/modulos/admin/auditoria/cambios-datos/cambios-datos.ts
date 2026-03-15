// src/app/modulos/admin/auditoria/cambios-datos/cambios-datos.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ToastComponent } from '../../../../component/toast.component';
import { LoadingSpinnerComponent } from '../../../../component/loading-spinner.component';
import { ToastService } from '../../../../services/toast.service';
import {
  AuditoriaCambiosService,
  AudCambio,
  FiltrosCambios
} from '../../../../services/auditoria-cambios.service';
import {
  ReporteAuditoriaService,
  ReporteAuditoriaConfig
} from '../../../../services/reporte-auditoria.service';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-cambios-datos',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    RouterLinkActive,
    ToastComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './cambios-datos.html',
  styleUrls: ['./cambios-datos.scss']
})
export class CambiosDatosComponent implements OnInit {

  registros:     AudCambio[] = [];
  totalElements  = 0;
  totalPages     = 0;
  currentPage    = 0;
  pageSize       = 20;
  isLoading      = false;

  filtros: FiltrosCambios = {
    tabla: '', operacion: '', campo: '', usuarioApp: '', desde: '', hasta: ''
  };

  detalle: AudCambio | null = null;

  // ── KPIs de hoy ─────────────────────────────────────────────────────────
  private readonly apiStats = `${environment.apiUrl}/admin/auditoria`;
  kpiHoy = { total: 0, inserts: 0, updates: 0, deletes: 0 };

  // ── Reporte ─────────────────────────────────────────────────────────────
  modalReporteAbierto = false;
  generandoReporte    = false;
  cfgReporte!: ReporteAuditoriaConfig;

  // Tablas auditadas (las que tienen trigger)
  readonly tablas = [
    'usuario',
    'solicitud_docente',
    'convocatoria',
    'prepostulacion',
    'postulacion',
    'proceso_evaluacion',
    'evaluacion',
    'documento'
  ];

  readonly operaciones = ['INSERT', 'UPDATE', 'DELETE'];

  constructor(
    private svc:        AuditoriaCambiosService,
    private reporteSvc: ReporteAuditoriaService,
    private http:       HttpClient,
    private cdr:        ChangeDetectorRef,
    private toast:      ToastService
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.cargarKpiHoy();
  }

  // ── Carga de datos ───────────────────────────────────────────────────────

  cargar(page = 0): void {
    this.isLoading = true;
    this.cdr.detectChanges();

    this.svc.listar(this.filtros, page, this.pageSize).subscribe({
      next: (data) => {
        this.registros     = data.content;
        this.totalElements = data.totalElements;
        this.totalPages    = data.totalPages;
        this.currentPage   = data.number;
        this.isLoading     = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.toast.error('Error al cargar', 'No se pudieron obtener los registros de cambios.');
        this.cdr.detectChanges();
      }
    });
  }

  aplicarFiltros(): void { this.cargar(0); }

  private parseJson(val: any): any[] {
    if (!val) return [];
    if (Array.isArray(val)) return val;
    if (typeof val === 'string') { try { return JSON.parse(val); } catch { return []; } }
    return [];
  }

  cargarKpiHoy(): void {
    const hoy = new Date().toISOString().slice(0, 10);
    this.http.get<any>(`${this.apiStats}/estadisticas/cambios`).subscribe({
      next: (data) => {
        const tendencia = this.parseJson(data.tendenciaDiaria);
        const fila = tendencia.find((r: any) => r.dia === hoy);
        if (fila) {
          this.kpiHoy = {
            total:   Number(fila.total   ?? 0),
            inserts: Number(fila.inserts ?? 0),
            updates: Number(fila.updates ?? 0),
            deletes: Number(fila.deletes ?? 0),
          };
        }
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  limpiarFiltros(): void {
    this.filtros = { tabla: '', operacion: '', campo: '', usuarioApp: '', desde: '', hasta: '' };
    this.cargar(0);
    this.toast.info('Filtros limpiados');
  }

  irPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.cargar(p);
  }

  verDetalle(r: AudCambio): void  { this.detalle = r; }
  cerrarDetalle(): void           { this.detalle = null; }

  // ── Reporte ──────────────────────────────────────────────────────────────

  abrirReporte(): void {
    // Pre-carga los filtros activos de la tabla en la config del reporte
    this.cfgReporte = this.reporteSvc.configDefecto({
      desde:      this.filtros.desde,
      hasta:      this.filtros.hasta,
      usuarioApp: this.filtros.usuarioApp,
      tabla:      this.filtros.tabla,
      operacion:  this.filtros.operacion,
    });
    // Desde cambios-datos el tipo por defecto es CAMBIOS
    this.cfgReporte.tipoReporte = 'CAMBIOS';
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

  // ── Helpers de visualización ─────────────────────────────────────────────

  formatFecha(fecha: string): string {
    return new Date(fecha).toLocaleString('es-EC', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
  }

  badgeOperacion(op: string): string {
    const map: Record<string, string> = {
      INSERT: 'badge-insert',
      UPDATE: 'badge-update',
      DELETE: 'badge-delete'
    };
    return map[op] ?? 'badge-default';
  }

  labelOperacion(op: string): string {
    const map: Record<string, string> = {
      INSERT: 'Insertar',
      UPDATE: 'Actualizar',
      DELETE: 'Eliminar'
    };
    return map[op] ?? op;
  }

  badgeTabla(tabla: string): string {
    const map: Record<string, string> = {
      usuario:            'badge-tabla-usuario',
      solicitud_docente:  'badge-tabla-solicitud',
      convocatoria:       'badge-tabla-conv',
      prepostulacion:     'badge-tabla-pre',
      postulacion:        'badge-tabla-post',
      proceso_evaluacion: 'badge-tabla-proceso',
      evaluacion:         'badge-tabla-eval',
      documento:          'badge-tabla-doc'
    };
    return map[tabla?.toLowerCase()] ?? 'badge-tabla-default';
  }

  esExterno(r: AudCambio): boolean {
    return !r.usuarioApp;
  }

  truncar(val: string | null, max = 40): string {
    if (!val) return '—';
    return val.length > max ? val.slice(0, max) + '…' : val;
  }

  get pages(): number[] {
    const cur = this.currentPage;
    const range: number[] = [];
    for (let i = Math.max(0, cur - 2); i <= Math.min(this.totalPages - 1, cur + 2); i++) {
      range.push(i);
    }
    return range;
  }
}
