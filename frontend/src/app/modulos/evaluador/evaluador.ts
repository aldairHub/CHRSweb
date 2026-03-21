import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

interface DashCard { titulo: string; descripcion: string; ruta: string; svgPaths: Array<{ d: string }>; }

const SVG_MAP: Record<string, Array<{ d: string }>> = {
  'postulantes':    [{ d: 'M16 14.667a5.333 5.333 0 100-10.667 5.333 5.333 0 000 10.667z' }, { d: 'M5.333 28v-2.667A5.333 5.333 0 0110.667 20h10.666A5.333 5.333 0 0126.667 25.333V28' }],
  'documentos':           [{ d: 'M18.667 2.667H8a2.667 2.667 0 00-2.667 2.666v21.334A2.667 2.667 0 008 29.333h16a2.667 2.667 0 002.667-2.666V10.667L18.667 2.667z' }, { d: 'M18.667 2.667v8h8' }, { d: 'M21.333 17.333H10.667' }, { d: 'M21.333 22.667H10.667' }, { d: 'M13.333 12H10.667' }],
  'matriz-meritos': [{ d: 'M6.667 4h18.666A2.667 2.667 0 0128 6.667v18.666A2.667 2.667 0 0125.333 28H6.667A2.667 2.667 0 014 25.333V6.667A2.667 2.667 0 016.667 4z' }, { d: 'M4 12h24' }, { d: 'M4 20h24' }, { d: 'M12 4v24' }, { d: 'M20 4v24' }],
  'reportes':       [{ d: 'M4 26.667h24' }, { d: 'M6.667 26.667V20A1.333 1.333 0 018 18.667h2.667A1.333 1.333 0 0112 20v6.667' }, { d: 'M12 26.667V13.333A1.333 1.333 0 0113.333 12H16a1.333 1.333 0 011.333 1.333v13.334' }, { d: 'M17.333 26.667V8A1.333 1.333 0 0118.667 6.667h2.666A1.333 1.333 0 0122.667 8v18.667' }],
  'solicitar': [{ d: 'M13.333 28H5.333A2.667 2.667 0 012.667 25.333V6.667A2.667 2.667 0 015.333 4h13.334l8 8v5.333' }, { d: 'M18.667 4v8h8' }, { d: 'M24 21.333v8' }, { d: 'M20 25.333h8' }],
  'entrevistas-docentes': [{ d: 'M26.667 5.333H5.333A2.667 2.667 0 002.667 8v13.333A2.667 2.667 0 005.333 24H8l4 4 4-4h10.667A2.667 2.667 0 0029.333 21.333V8a2.667 2.667 0 00-2.666-2.667z' }, { d: 'M10.667 13.333h10.666' }, { d: 'M10.667 18.667h6.666' }],
};
const SVG_FALLBACK: Array<{ d: string }> = [{ d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' }, { d: 'M16 10.6667V16' }, { d: 'M16 21.3333H16.0133' }];
const QUICK_KEY = 'dashboard_evaluador_quick';

@Component({ selector: 'app-evaluador', standalone: true, imports: [CommonModule], templateUrl: './evaluador.html', styleUrls: ['./evaluador.scss'] })
export class EvaluadorComponent implements OnInit {
  cards: DashCard[] = [];
  stats: any = {};
  actividadReciente: any[] = [];
  cargandoStats = true;
  quickAccesos: string[] = [];
  editandoQuick = false;

  constructor(private router: Router, private http: HttpClient, private authService: AuthService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.construirCards();
    this.cargarStats();
    try { this.quickAccesos = JSON.parse(localStorage.getItem(QUICK_KEY) || '[]'); } catch { this.quickAccesos = []; }
  }

  private construirCards(): void {
    const m = this.authService.getModulo();
    if (!m?.opciones?.length) { this.cards = []; return; }
    this.cards = m.opciones.map((op: any) => {
      console.log('Rutas evaluador:', m.opciones.map((op: any) => op.ruta));
      const k = (op.ruta || '').replace(/^\//, '').split('/').pop() ?? '';
      return { titulo: op.nombre, descripcion: op.descripcion || '', ruta: k, svgPaths: SVG_MAP[k] ?? SVG_FALLBACK };
    });
  }

  private cargarStats(): void {
    const token = localStorage.getItem('token');
    const headers = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : undefined;
    this.http.get<any>(`${environment.apiUrl}/dashboard/evaluador`, headers ? { headers } : {}).subscribe({
      next: (d) => { this.stats = d; this.actividadReciente = d.actividadReciente || []; this.cargandoStats = false; this.cdr.detectChanges(); },
      error: () => { this.cargandoStats = false; }
    });
  }

  getActColor(a: string): string {
    return ({ INSERT: 'green', UPDATE: 'blue', DELETE: 'red', CREAR: 'green', ACTUALIZAR: 'blue', ELIMINAR: 'red', CAMBIAR_ESTADO: 'amber' } as any)[a?.toUpperCase()] ?? 'gray';
  }

  getMaxPipeline(): number {
    return Math.max(
      this.stats.solicitudesActivas ?? 0,
      this.stats.entrevistasProgramadas ?? 0,
      this.stats.evaluacionesCompletadas ?? 0,
      this.stats.procesosActivos ?? 0,
      1
    );
  }

  getPipelineWidth(val: number | undefined, max: number): number {
    const v = val ?? 0;
    return max > 0 ? Math.round((v / max) * 100) : 0;
  }

  getActCount(accion: string): number {
    return this.actividadReciente.filter(i => i.accion?.toUpperCase() === accion.toUpperCase()).length;
  }

  getTopUsuarios(): { usuario: string; count: number }[] { return []; }
  getInitials(n: string): string { return ''; }
  getBarWidth(c: number, m: number): number { return 0; }
  isQuickSelected(r: string) { return this.quickAccesos.includes(r); }
  toggleQuickItem(r: string) { this.quickAccesos = this.isQuickSelected(r) ? this.quickAccesos.filter(x => x !== r) : this.quickAccesos.length < 4 ? [...this.quickAccesos, r] : this.quickAccesos; localStorage.setItem(QUICK_KEY, JSON.stringify(this.quickAccesos)); }
  getQuickCards(): DashCard[] { return this.quickAccesos.map(r => this.cards.find(c => c.ruta === r)).filter((c): c is DashCard => !!c); }
  toggleConfigQuick() { this.editandoQuick = !this.editandoQuick; }
  navegarA(ruta: string) { this.router.navigate(['/evaluador', ruta]); }
}
