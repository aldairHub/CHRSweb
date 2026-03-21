import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { environment } from '../../../../environments/environment';

interface SolicitudItem {
  idSolicitud: number; nombreFacultad: string; nombreCarrera: string;
  modalidadCarrera: string; nombreMateria: string; nombreArea: string;
  estadoSolicitud: string; fechaSolicitud: string; cantidadDocentes: number;
  nivelAcademico: string; experienciaProfesionalMin: number;
  experienciaDocenteMin: number; nombreAutoridad: string;
}

interface Stats {
  total: number; pendientes: number; aprobadas: number; rechazadas: number;
  docentesRequeridos: number; docentesAprobados: number; docentesPendientes: number;
  tasaAprobacion: number; cobertura: number;
  expProfPromedio: number; expDocPromedio: number;
  porFacultad: { nombre: string; count: number; aprobadas: number; docentes: number }[];
  porArea: { nombre: string; count: number }[];
  porNivel: { nivel: string; count: number; pct: number }[];
  porModalidad: { modalidad: string; count: number }[];
  masAntiguas: SolicitudItem[];
  topAutoridades: { nombre: string; count: number; aprobadas: number }[];
  distribucion: { label: string; val: number; pct: number; color: string }[];
}

@Component({
  selector: 'app-estadisticas-solicitudes',
  standalone: true,
  imports: [CommonModule, FormsModule, ToastComponent],
  templateUrl: './estadisticas-solicitudes.html',
  styleUrls: ['./estadisticas-solicitudes.scss']
})
export class EstadisticasSolicitudesComponent implements OnInit {

  private readonly API = environment.apiUrl;
  cargando = false; cargandoIA = false; analisisIA = '';
  datos: SolicitudItem[] = []; stats: Stats | null = null; ultimaAct = '';

  showExport = false; exportando = false;
  exportCfg = {
    formato: 'PDF' as 'PDF'|'EXCEL', orientacion: 'HORIZONTAL' as 'VERTICAL'|'HORIZONTAL',
    titulo: 'Estadísticas de Solicitudes de Docente', subtitulo: '', colorPrimario: '#00A63E',
    estado: '', facultad: '', desde: '', hasta: '',
    incluirPortada: true, incluirKpis: true, incluirDetalle: true,
    incluirGraficoEstados: true, incluirGraficoCarreras: true,
    incluirGraficoAreas: true, incluirGraficoTemporal: true,
    mostrarNumeroPagina: true, mostrarFechaGeneracion: true,
    excelCongelarEncabezado: true, excelFiltrosAutomaticos: true,
  };

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef,
              private router: Router, private toast: ToastService) {}

  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    this.cargando = true;
    this.http.get<SolicitudItem[]>(`${this.API}/solicitudes-docente`).subscribe({
      next: d => {
        this.datos = Array.isArray(d) ? d : [];
        this.calcular();
        this.cargando = false;
        this.ultimaAct = new Date().toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' });
        this.cdr.detectChanges();
      },
      error: () => { this.cargando = false; this.toast.error('Error', 'No se pudieron cargar las solicitudes.'); this.cdr.detectChanges(); }
    });
  }

  private calcular(): void {
    const d = this.datos;
    if (!d.length) { this.stats = null; return; }
    const pendientes = d.filter(s => s.estadoSolicitud === 'pendiente').length;
    const aprobadas  = d.filter(s => s.estadoSolicitud === 'aprobada').length;
    const rechazadas = d.filter(s => s.estadoSolicitud === 'rechazada').length;
    const docentesReq  = d.reduce((s, x) => s + (x.cantidadDocentes ?? 0), 0);
    const docentesApr  = d.filter(x => x.estadoSolicitud === 'aprobada').reduce((s, x) => s + (x.cantidadDocentes ?? 0), 0);
    const revisadas = aprobadas + rechazadas;
    const tasaAprobacion = revisadas > 0 ? Math.round(aprobadas / revisadas * 100) : 0;
    const cobertura = docentesReq > 0 ? Math.round(docentesApr / docentesReq * 100) : 0;
    const expProfPromedio = d.length ? +(d.reduce((s, x) => s + (x.experienciaProfesionalMin ?? 0), 0) / d.length).toFixed(1) : 0;
    const expDocPromedio  = d.length ? +(d.reduce((s, x) => s + (x.experienciaDocenteMin ?? 0), 0) / d.length).toFixed(1) : 0;

    const byFac: Record<string, { count: number; aprobadas: number; docentes: number }> = {};
    d.forEach(s => {
      const f = s.nombreFacultad || 'Sin facultad';
      if (!byFac[f]) byFac[f] = { count: 0, aprobadas: 0, docentes: 0 };
      byFac[f].count++;
      if (s.estadoSolicitud === 'aprobada') { byFac[f].aprobadas++; byFac[f].docentes += s.cantidadDocentes ?? 0; }
    });
    const porFacultad = Object.entries(byFac).map(([nombre, v]) => ({ nombre, ...v })).sort((a, b) => b.count - a.count).slice(0, 8);

    const byArea: Record<string, number> = {};
    d.forEach(s => { const a = s.nombreArea || 'Sin área'; byArea[a] = (byArea[a] ?? 0) + 1; });
    const porArea = Object.entries(byArea).map(([nombre, count]) => ({ nombre, count })).sort((a, b) => b.count - a.count).slice(0, 7);

    const byNivel: Record<string, number> = {};
    d.forEach(s => { const n = s.nivelAcademico || 'No especificado'; byNivel[n] = (byNivel[n] ?? 0) + 1; });
    const porNivel = Object.entries(byNivel).map(([nivel, count]) => ({
      nivel, count, pct: Math.round(count / d.length * 100)
    })).sort((a, b) => b.count - a.count);

    const byMod: Record<string, number> = {};
    d.forEach(s => { const m = s.modalidadCarrera || 'No especificada'; byMod[m] = (byMod[m] ?? 0) + 1; });
    const porModalidad = Object.entries(byMod).map(([modalidad, count]) => ({ modalidad, count })).sort((a, b) => b.count - a.count);

    // Top autoridades
    const byAuth: Record<string, { count: number; aprobadas: number }> = {};
    d.forEach(s => {
      const a = s.nombreAutoridad || 'Desconocido';
      if (!byAuth[a]) byAuth[a] = { count: 0, aprobadas: 0 };
      byAuth[a].count++;
      if (s.estadoSolicitud === 'aprobada') byAuth[a].aprobadas++;
    });
    const topAutoridades = Object.entries(byAuth).map(([nombre, v]) => ({ nombre, ...v })).sort((a, b) => b.count - a.count).slice(0, 5);

    // Pendientes más antiguas
    const ahora = Date.now();
    const masAntiguas = d.filter(s => s.estadoSolicitud === 'pendiente' && s.fechaSolicitud)
      .sort((a, b) => new Date(a.fechaSolicitud).getTime() - new Date(b.fechaSolicitud).getTime()).slice(0, 6);

    const distribucion = [
      { label: 'Aprobadas',  val: aprobadas,  pct: d.length ? Math.round(aprobadas  / d.length * 100) : 0, color: '#16a34a' },
      { label: 'Pendientes', val: pendientes, pct: d.length ? Math.round(pendientes / d.length * 100) : 0, color: '#d97706' },
      { label: 'Rechazadas', val: rechazadas, pct: d.length ? Math.round(rechazadas / d.length * 100) : 0, color: '#dc2626' },
    ];

    this.stats = {
      total: d.length, pendientes, aprobadas, rechazadas,
      docentesRequeridos: docentesReq, docentesAprobados: docentesApr,
      docentesPendientes: docentesReq - docentesApr,
      tasaAprobacion, cobertura, expProfPromedio, expDocPromedio,
      porFacultad, porArea, porNivel, porModalidad, masAntiguas, topAutoridades, distribucion,
    };
  }

  generarIA(): void {
    if (this.cargandoIA || !this.stats) return;
    this.cargandoIA = true; this.analisisIA = '';
    this.http.post<{ analisis: string }>(`${this.API}/revisor/reportes/analisis-ia`, { solicitudes: this.stats }).subscribe({
      next: r => { this.analisisIA = r.analisis ?? this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); },
      error: () => { this.analisisIA = this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); }
    });
  }
  private fallbackIA(): string {
    const s = this.stats!;
    let t = `El sistema registra ${s.total} solicitud${s.total !== 1 ? 'es' : ''} de docente que representan una demanda total de `;
    t += `${s.docentesRequeridos} docente${s.docentesRequeridos !== 1 ? 's' : ''}. `;
    t += `De estas, ${s.aprobadas} han sido aprobadas (${s.docentesAprobados} docentes cubiertos), `;
    t += `${s.pendientes} están pendientes de revisión y ${s.rechazadas} fueron rechazadas. `;
    t += `La tasa de aprobación sobre revisadas es del ${s.tasaAprobacion}% y la cobertura de docentes alcanza el ${s.cobertura}%. `;
    if (s.docentesPendientes > 0) t += `Aún quedan ${s.docentesPendientes} plaza${s.docentesPendientes > 1 ? 's' : ''} de docente sin cubrir. `;
    if (s.porFacultad.length > 0) {
      t += `La facultad con mayor demanda es "${s.porFacultad[0].nombre}" con ${s.porFacultad[0].count} solicitudes`;
      if (s.porFacultad.length > 1) t += `; le sigue "${s.porFacultad[1].nombre}" con ${s.porFacultad[1].count}`;
      t += '. ';
    }
    if (s.expProfPromedio > 0 || s.expDocPromedio > 0)
      t += `Los perfiles requeridos exigen en promedio ${s.expProfPromedio} año${s.expProfPromedio !== 1 ? 's' : ''} de experiencia profesional y ${s.expDocPromedio} de docente. `;
    if (s.masAntiguas.length > 0) {
      t += `⚠ Hay ${s.masAntiguas.length} solicitud${s.masAntiguas.length > 1 ? 'es' : ''} pendiente${s.masAntiguas.length > 1 ? 's' : ''} con alta antigüedad; `;
      t += `su resolución debería priorizarse para evitar vacíos en la carga horaria. `;
    }
    if (s.pendientes > s.aprobadas)
      t += `El volumen pendiente supera al aprobado; se recomienda agilizar la revisión de las solicitudes más antiguas. `;
    if (s.cobertura >= 80)
      t += `La cobertura del ${s.cobertura}% indica que la mayoría de las necesidades académicas están siendo atendidas satisfactoriamente.`;
    return t.trim();
  }

  abrirExport(): void { this.showExport = true; }
  cerrarExport(): void { if (!this.exportando) this.showExport = false; }
  exportar(): void {
    this.exportando = true;
    this.http.post(`${this.API}/admin/solicitudes-docentes/reporte/generar`, this.exportCfg, { responseType: 'blob', observe: 'response' }).subscribe({
      next: resp => {
        const ext = this.exportCfg.formato === 'EXCEL' ? 'xlsx' : 'pdf';
        const cd = resp.headers.get('content-disposition') ?? '';
        const nombre = (cd.match(/filename="?([^";\n]+)"?/) || [])[1] ?? `solicitudes.${ext}`;
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
  maxFac(): number  { return Math.max(...(this.stats?.porFacultad.map(f => f.count) ?? [1]), 1); }
  maxArea(): number { return Math.max(...(this.stats?.porArea.map(a => a.count) ?? [1]), 1); }
  maxAuth(): number { return Math.max(...(this.stats?.topAutoridades.map(a => a.count) ?? [1]), 1); }
  diasPend(f: string): number { return !f ? 0 : Math.round((Date.now() - new Date(f).getTime()) / 86400000); }
  formatFecha(f: string): string { return !f ? '' : new Date(f).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: '2-digit' }); }
  gaugeColor(pct: number): string { return pct >= 75 ? '#16a34a' : pct >= 45 ? '#d97706' : '#dc2626'; }
  gaugeOffset(pct: number): number { return 220 - 220 * Math.max(0, Math.min(100, pct)) / 100; }
  colorIdx(i: number): string { return ['#00A63E','#2563eb','#d97706','#7c3aed','#0891b2','#dc2626','#059669','#be185d'][i % 8]; }
  badgeClass(e: string): string { return ({ aprobada:'b-ok', rechazada:'b-danger', pendiente:'b-warn' } as any)[e] ?? ''; }
  labelEst(e: string): string { return ({ aprobada:'Aprobada', rechazada:'Rechazada', pendiente:'Pendiente' } as any)[e] ?? e; }
  volver(): void { this.router.navigate(['/revisor/solicitudes-docente']); }
}
