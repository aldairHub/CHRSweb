import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

interface DashCard { titulo: string; descripcion: string; ruta: string; svgPaths: Array<{ d: string }>; }

const SVG_MAP: Record<string, Array<{ d: string }>> = {
  'subir-documentos': [{ d: 'M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12' }],
  'entrevista':       [{ d: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z' }],
  'resultados':       [{ d: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' }],
  'convocatorias':    [{ d: 'M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z' }],
};
const SVG_FALLBACK: Array<{ d: string }> = [{ d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' }, { d: 'M16 10.6667V16' }, { d: 'M16 21.3333H16.0133' }];
const QUICK_KEY = 'dashboard_postulante_quick';

@Component({ selector: 'app-postulante', standalone: true, imports: [CommonModule], templateUrl: './postulante.html', styleUrls: ['./postulante.scss'] })
export class PostulanteComponent implements OnInit {
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

  private rutaDesdeNombre(nombre: string): string {
    const n = (nombre || '').toLowerCase().trim();
    if (n.includes('entrevista'))                          return 'entrevista';
    if (n.includes('document') || n.includes('subir'))    return 'subir-documentos';
    if (n.includes('resultado'))                           return 'resultados';
    if (n.includes('convocatoria'))                        return 'convocatorias';
    if (n.includes('postulacion') || n.includes('nueva')) return 'nueva-postulacion';
    return n.replace(/\s+/g, '-');
  }

  private construirCards(): void {
    const m = this.authService.getModulo();
    if (!m?.opciones?.length) {
      this.cards = [
        { titulo: 'Convocatorias',    descripcion: 'Ver plazas abiertas y postular', ruta: 'convocatorias',    svgPaths: SVG_MAP['convocatorias'] },
        { titulo: 'Subir documentos', descripcion: 'Sube tus documentos requeridos', ruta: 'subir-documentos', svgPaths: SVG_MAP['subir-documentos'] },
        { titulo: 'Entrevista',       descripcion: 'Ver tus entrevistas',             ruta: 'entrevista',       svgPaths: SVG_MAP['entrevista'] },
        { titulo: 'Resultados',       descripcion: 'Ver resultados del proceso',      ruta: 'resultados',       svgPaths: SVG_MAP['resultados'] },
      ];
      return;
    }
    const cardsBackend: DashCard[] = m.opciones.map((op: any) => {
      const ruta = this.rutaDesdeNombre(op.nombre);
      return { titulo: op.nombre, descripcion: op.descripcion || '', ruta, svgPaths: SVG_MAP[ruta] ?? SVG_FALLBACK };
    });
    const tieneConvocatorias = cardsBackend.some(c => c.ruta === 'convocatorias');
    if (!tieneConvocatorias) {
      this.cards = [
        { titulo: 'Convocatorias', descripcion: 'Ver plazas abiertas y postular', ruta: 'convocatorias', svgPaths: SVG_MAP['convocatorias'] },
        ...cardsBackend
      ];
    } else {
      this.cards = cardsBackend;
    }
  }

  private cargarStats(): void {
    const token    = localStorage.getItem('token');
    const usuario  = localStorage.getItem('usuario') ?? '';
    const headers  = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : undefined;
    const params   = usuario ? { params: { usuarioApp: usuario } } : {};
    const options  = headers ? { headers, ...params } : params;
    this.http.get<any>(`${environment.apiUrl}/dashboard/postulante`, options).subscribe({
      next: (d) => { this.stats = d; this.actividadReciente = d.actividadReciente || []; this.cargandoStats = false; this.cdr.detectChanges(); },
      error: () => { this.cargandoStats = false; }
    });
  }

  getActColor(a: string): string {
    return ({ INSERT: 'green', UPDATE: 'blue', DELETE: 'red', CREAR: 'green', ACTUALIZAR: 'blue', ELIMINAR: 'red', CAMBIAR_ESTADO: 'amber' } as any)[a?.toUpperCase()] ?? 'gray';
  }

  getStepClass(step: string): string {
    const s = this.stats;
    if (step === 'convocatoria') return (s.convocatoriasAbiertas ?? 0) > 0 ? 'post-step--active' : 'post-step--idle';
    if (step === 'proceso')      return (s.procesosEnCurso ?? 0) > 0 ? 'post-step--progress' : 'post-step--idle';
    if (step === 'entrevista')   return (s.entrevistasHoy ?? 0) > 0 ? 'post-step--today' : 'post-step--idle';
    return 'post-step--idle';
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

  navegarA(ruta: string) {
    if (ruta === 'convocatorias') {
      this.router.navigate(['/convocatorias']);
    } else {
      this.router.navigate([`/postulante/${ruta}`]);
    }
  }
}
