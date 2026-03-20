import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';
import { environment } from '../../../../environments/environment';

interface SolicitudItem {
  idSolicitud: number;
  nombreFacultad: string;
  nombreCarrera: string;
  modalidadCarrera: string;
  nombreMateria: string;
  nombreArea: string;
  estadoSolicitud: string;
  fechaSolicitud: string;
  cantidadDocentes: number;
  nivelAcademico: string;
  experienciaProfesionalMin: number;
  experienciaDocenteMin: number;
  nombreAutoridad: string;
}

interface Stats {
  total: number;
  pendientes: number;
  aprobadas: number;
  rechazadas: number;
  docentesRequeridos: number;
  docentesAprobados: number;
  tasaAprobacion: number;
  porFacultad: { nombre: string; count: number; aprobadas: number; docentes: number }[];
  porArea: { nombre: string; count: number }[];
  porNivel: { nivel: string; count: number }[];
  porModalidad: { modalidad: string; count: number }[];
  masAntiguas: SolicitudItem[];
  topDocentes: { nombre: string; count: number }[];
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

  cargando = false;
  cargandoIA = false;
  analisisIA = '';
  datos: SolicitudItem[] = [];
  stats: Stats | null = null;
  ultimaActualizacion = '';

  sec = {
    kpis: true,
    facultad: true,
    area: true,
    nivel: true,
    modalidad: false,
    pendientes: true,
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
    this.http.get<SolicitudItem[]>(`${this.API}/solicitudes-docente`).subscribe({
      next: data => {
        this.datos = Array.isArray(data) ? data : [];
        this.calcularStats();
        this.cargando = false;
        this.ultimaActualizacion = new Date().toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' });
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('Error', 'No se pudieron cargar las solicitudes.');
        this.cdr.detectChanges();
      }
    });
  }

  private calcularStats(): void {
    const d = this.datos;
    const pendientes = d.filter(s => s.estadoSolicitud === 'pendiente').length;
    const aprobadas  = d.filter(s => s.estadoSolicitud === 'aprobada').length;
    const rechazadas = d.filter(s => s.estadoSolicitud === 'rechazada').length;
    const docentesRequeridos = d.reduce((sum, s) => sum + (s.cantidadDocentes ?? 0), 0);
    const docentesAprobados  = d.filter(s => s.estadoSolicitud === 'aprobada')
      .reduce((sum, s) => sum + (s.cantidadDocentes ?? 0), 0);
    const revisadas = aprobadas + rechazadas;
    const tasaAprobacion = revisadas > 0 ? Math.round(aprobadas / revisadas * 100) : 0;

    // Por facultad
    const byFac: Record<string, { count: number; aprobadas: number; docentes: number }> = {};
    d.forEach(s => {
      const f = s.nombreFacultad || 'Sin facultad';
      if (!byFac[f]) byFac[f] = { count: 0, aprobadas: 0, docentes: 0 };
      byFac[f].count++;
      if (s.estadoSolicitud === 'aprobada') { byFac[f].aprobadas++; byFac[f].docentes += s.cantidadDocentes ?? 0; }
    });
    const porFacultad = Object.entries(byFac)
      .map(([nombre, v]) => ({ nombre, ...v }))
      .sort((a, b) => b.count - a.count).slice(0, 8);

    // Por área
    const byArea: Record<string, number> = {};
    d.forEach(s => { const a = s.nombreArea || 'Sin área'; byArea[a] = (byArea[a] ?? 0) + 1; });
    const porArea = Object.entries(byArea)
      .map(([nombre, count]) => ({ nombre, count }))
      .sort((a, b) => b.count - a.count).slice(0, 7);

    // Por nivel académico
    const byNivel: Record<string, number> = {};
    d.forEach(s => { const n = s.nivelAcademico || 'No especificado'; byNivel[n] = (byNivel[n] ?? 0) + 1; });
    const porNivel = Object.entries(byNivel)
      .map(([nivel, count]) => ({ nivel, count }))
      .sort((a, b) => b.count - a.count);

    // Por modalidad
    const byMod: Record<string, number> = {};
    d.forEach(s => { const m = s.modalidadCarrera || 'No especificada'; byMod[m] = (byMod[m] ?? 0) + 1; });
    const porModalidad = Object.entries(byMod)
      .map(([modalidad, count]) => ({ modalidad, count }))
      .sort((a, b) => b.count - a.count);

    // Solicitudes pendientes más antiguas
    const masAntiguas = d
      .filter(s => s.estadoSolicitud === 'pendiente' && s.fechaSolicitud)
      .sort((a, b) => a.fechaSolicitud.localeCompare(b.fechaSolicitud))
      .slice(0, 5);

    // Top autoridades por solicitudes
    const byAuth: Record<string, number> = {};
    d.forEach(s => { const a = s.nombreAutoridad || 'Desconocido'; byAuth[a] = (byAuth[a] ?? 0) + 1; });
    const topDocentes = Object.entries(byAuth)
      .map(([nombre, count]) => ({ nombre, count }))
      .sort((a, b) => b.count - a.count).slice(0, 5);

    this.stats = {
      total: d.length, pendientes, aprobadas, rechazadas,
      docentesRequeridos, docentesAprobados, tasaAprobacion,
      porFacultad, porArea, porNivel, porModalidad,
      masAntiguas, topDocentes,
    };
  }

  generarIA(): void {
    if (this.cargandoIA || !this.stats) return;
    this.cargandoIA = true; this.analisisIA = '';
    this.http.post<{ analisis: string }>(`${this.API}/revisor/reportes/analisis-ia`, {
      solicitudes: this.stats
    }).subscribe({
      next: r => { this.analisisIA = r.analisis ?? this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); },
      error: () => { this.analisisIA = this.fallbackIA(); this.cargandoIA = false; this.cdr.detectChanges(); }
    });
  }

  private fallbackIA(): string {
    const s = this.stats!;
    let t = `Se registran ${s.total} solicitud${s.total !== 1 ? 'es' : ''} de docente en total, `;
    t += `representando una demanda de ${s.docentesRequeridos} docente${s.docentesRequeridos !== 1 ? 's' : ''}. `;
    t += `De estas, ${s.aprobadas} han sido aprobadas (${s.docentesAprobados} docentes cubiertos) y ${s.pendientes} están pendientes de revisión. `;
    if (s.porFacultad.length > 0) t += `La facultad con mayor demanda es "${s.porFacultad[0].nombre}" con ${s.porFacultad[0].count} solicitudes. `;
    if (s.masAntiguas.length > 0) t += `⚠ Hay ${s.masAntiguas.length} solicitud${s.masAntiguas.length > 1 ? 'es' : ''} pendiente${s.masAntiguas.length > 1 ? 's' : ''} con mayor antigüedad que requieren atención prioritaria. `;
    if (s.pendientes > s.aprobadas) t += 'El volumen de solicitudes pendientes supera a las aprobadas; se recomienda agilizar el proceso de revisión. ';
    return t.trim();
  }

  toggle(k: keyof typeof this.sec) { this.sec[k] = !this.sec[k]; }

  barWidth(v: number, max: number) { return (!max ? 0 : Math.max(Math.round(v / max * 100), 2)) + '%'; }
  maxFac()  { return Math.max(...(this.stats?.porFacultad.map(f => f.count) ?? [1]), 1); }
  maxArea() { return Math.max(...(this.stats?.porArea.map(a => a.count)    ?? [1]), 1); }
  maxNivel(){ return Math.max(...(this.stats?.porNivel.map(n => n.count)   ?? [1]), 1); }
  maxMod()  { return Math.max(...(this.stats?.porModalidad.map(m => m.count) ?? [1]), 1); }

  diasPendiente(fecha: string): number {
    if (!fecha) return 0;
    return Math.round((Date.now() - new Date(fecha).getTime()) / 86400000);
  }
  formatFecha(f: string) {
    if (!f) return '';
    return new Date(f).toLocaleDateString('es-EC', { day: '2-digit', month: 'short', year: '2-digit' });
  }
  colorFacultad(i: number): string {
    const cols = ['#00A63E','#2563eb','#d97706','#7c3aed','#0891b2','#dc2626','#059669','#be185d'];
    return cols[i % cols.length];
  }
  donutOffset(pct: number) { return 175.9 - 175.9 * Math.max(0, Math.min(100, pct)) / 100; }
  classBadge(e: string) {
    return ({ aprobada: 'badge-ok', rechazada: 'badge-danger', pendiente: 'badge-warn' } as any)[e] ?? '';
  }
  labelEstado(e: string) {
    return ({ aprobada: 'Aprobada', rechazada: 'Rechazada', pendiente: 'Pendiente' } as any)[e] ?? e;
  }

  volver() { this.router.navigate(['/revisor/solicitudes-docente']); }
  irReportes() { this.router.navigate(['/revisor/reportes']); }
}
