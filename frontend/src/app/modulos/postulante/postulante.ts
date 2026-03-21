import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

interface DashCard { titulo: string; descripcion: string; ruta: string; svgPaths: Array<{ d: string }>; }

const SVG_MAP: Record<string, Array<{ d: string }>> = {
  'convocatorias':    [{ d: 'M6.667 6.667h18.666a2.667 2.667 0 012.667 2.666v5.334H4V9.333a2.667 2.667 0 012.667-2.666z' }, { d: 'M4 14.667h24V28H4z' }, { d: 'M10.667 2.667v5.333' }, { d: 'M21.333 2.667v5.333' }, { d: 'M9.333 21.333h5.334' }, { d: 'M9.333 25.333h8' }],
  'subir-documentos': [{ d: 'M26.667 20v5.333A2.667 2.667 0 0124 28H8a2.667 2.667 0 01-2.667-2.667V20' }, { d: 'M21.333 10.667L16 5.333l-5.333 5.334' }, { d: 'M16 5.333v16' }],
  'resultados':       [{ d: 'M4 26.667h24' }, { d: 'M6.667 26.667V20A1.333 1.333 0 018 18.667h2.667A1.333 1.333 0 0112 20v6.667' }, { d: 'M12 26.667V13.333A1.333 1.333 0 0113.333 12H16a1.333 1.333 0 011.333 1.333v13.334' }, { d: 'M17.333 26.667V8A1.333 1.333 0 0118.667 6.667h2.666A1.333 1.333 0 0122.667 8v18.667' }],
  'entrevista':       [{ d: 'M16 4a5.333 5.333 0 100 10.667A5.333 5.333 0 0016 4z' }, { d: 'M26.667 28c0-5.891-4.776-10.667-10.667-10.667S5.333 22.109 5.333 28' }, { d: 'M21.333 12l2.667 2.667L29.333 8' }],
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
