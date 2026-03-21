import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

interface DashCard { titulo: string; descripcion: string; ruta: string; svgPaths: Array<{ d: string }>; }

const SVG_MAP: Record<string, Array<{ d: string }>> = {
  'prepostulaciones':   [{ d: 'M21.333 28V25.333A5.333 5.333 0 0016 20H8a5.333 5.333 0 00-5.333 5.333V28' }, { d: 'M12 14.667a5.333 5.333 0 100-10.667 5.333 5.333 0 000 10.667z' }],
  'convocatorias':      [{ d: 'M26.667 5.333H5.333A2.667 2.667 0 002.667 8v18.667A2.667 2.667 0 005.333 29.333h21.334A2.667 2.667 0 0029.333 26.667V8a2.667 2.667 0 00-2.666-2.667z' }, { d: 'M21.333 2.667v5.333' }, { d: 'M10.667 2.667v5.333' }, { d: 'M2.667 13.333h26.666' }],
  'solicitudes-docente': [{ d: 'M18.667 4H8A2.667 2.667 0 005.333 6.667v18.666A2.667 2.667 0 008 28h16a2.667 2.667 0 002.667-2.667V13.333L18.667 4z' }, { d: 'M18.667 4v9.333h9.333' }, { d: 'M10.667 18.667h10.666' }, { d: 'M10.667 24h6.666' }],
  'config-matriz':      [{ d: 'M5.333 5.333h21.334A2.667 2.667 0 0129.333 8v16a2.667 2.667 0 01-2.666 2.667H5.333A2.667 2.667 0 012.667 24V8a2.667 2.667 0 012.666-2.667z' }, { d: 'M2.667 13.333h26.666' }, { d: 'M2.667 20h26.666' }, { d: 'M10.667 5.333v21.334' }, { d: 'M18.667 5.333v21.334' }],
  'config-entrevistas': [{ d: 'M26.667 5.333H5.333A2.667 2.667 0 002.667 8v13.333A2.667 2.667 0 005.333 24H8l4 4.667L16 24h10.667A2.667 2.667 0 0029.333 21.333V8a2.667 2.667 0 00-2.666-2.667z' }, { d: 'M10.667 14.667h10.666' }, { d: 'M10.667 18.667h5.333' }],
  'decisiones':         [{ d: 'M16 2.667a13.333 13.333 0 100 26.666A13.333 13.333 0 0016 2.667z' }, { d: 'M10.667 16l3.333 3.333 6.667-6.666' }],
  'convocatoria':       [{ d: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' }],
  'gestionpostulante':  [{ d: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' }],
  'reportes':           [{ d: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' }],
};
const SVG_FALLBACK: Array<{ d: string }> = [{ d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' }, { d: 'M16 10.6667V16' }, { d: 'M16 21.3333H16.0133' }];
const QUICK_KEY = 'dashboard_revisor_quick';

@Component({ selector: 'app-revisor', standalone: true, imports: [CommonModule], templateUrl: './revisor.html', styleUrls: ['./revisor.scss'] })
export class RevisorComponent implements OnInit {
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
    console.log('Rutas:', m.opciones.map((op: any) => op.ruta));
    this.cards = m.opciones.map((op: any) => {
      const k = (op.ruta || '').replace(/^\//, '').split('/').pop() ?? '';
      return { titulo: op.nombre, descripcion: op.descripcion || '', ruta: k, svgPaths: SVG_MAP[k] ?? SVG_FALLBACK };
    });
  }

  private cargarStats(): void {
    const token = localStorage.getItem('token');
    const headers = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : undefined;
    this.http.get<any>(`${environment.apiUrl}/dashboard/revisor`, headers ? { headers } : {}).subscribe({
      next: (d) => { this.stats = d; this.actividadReciente = d.actividadReciente || []; this.cargandoStats = false; this.cdr.detectChanges(); },
      error: () => { this.cargandoStats = false; }
    });
  }

  getActColor(a: string): string {
    return ({ INSERT: 'green', UPDATE: 'blue', DELETE: 'red', CREAR: 'green', ACTUALIZAR: 'blue', ELIMINAR: 'red', CAMBIAR_ESTADO: 'amber', APROBAR: 'green', RECHAZAR: 'red' } as any)[a?.toUpperCase()] ?? 'gray';
  }

  getTasaAprobacion(): number {
    const ap = this.stats.aprobadas ?? 0;
    const re = this.stats.rechazadas ?? 0;
    const total = ap + re;
    return total > 0 ? Math.round((ap / total) * 100) : 0;
  }

  getRingOffset(): number {
    const circ = 169.6;
    return circ - (circ * this.getTasaAprobacion() / 100);
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
  navegarA(ruta: string) { this.router.navigateByUrl('/revisor/' + ruta); }
}
