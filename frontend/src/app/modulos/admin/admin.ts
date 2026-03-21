import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { LogoService } from '../../services/logo.service';
import { environment } from '../../../environments/environment';

interface DashCard { titulo: string; descripcion: string; ruta: string; svgPaths: Array<{ d: string }>; }

interface DashStats {
  totalUsuarios?:          number;
  postulantesPendientes?:  number;
  convocatoriasAbiertas?:  number;
  totalConvocatorias?:     number;
  loginsHoy?:              number;
}

const SVG_MAP: Record<string, Array<{ d: string }>> = {
  'gestion-usuarios':  [{ d: 'M21.3334 28V25.3333C21.3334 23.9188 20.7715 22.5623 19.7713 21.5621C18.7711 20.5619 17.4146 20 16.0001 20H8.00008C6.58559 20 5.22904 20.5619 4.22885 21.5621C3.22865 22.5623 2.66675 23.9188 2.66675 25.3333V28' }, { d: 'M12.0001 14.6667C14.9456 14.6667 17.3334 12.2789 17.3334 9.33333C17.3334 6.38781 14.9456 4 12.0001 4C9.05456 4 6.66675 6.38781 6.66675 9.33333C6.66675 12.2789 9.05456 14.6667 12.0001 14.6667Z' }, { d: 'M29.3333 28V25.3333C29.3324 24.1516 28.9391 23.0037 28.2151 22.0698C27.4911 21.1358 26.4774 20.4688 25.3333 20.1733' }, { d: 'M21.3333 4.17334C22.4805 4.46707 23.4973 5.13427 24.2234 6.06975C24.9496 7.00523 25.3437 8.15578 25.3437 9.34001C25.3437 10.5242 24.9496 11.6748 24.2234 12.6103C23.4973 13.5457 22.4805 14.2129 21.3333 14.5067' }],
  'gestion-roles':     [{ d: 'M16 2.6667L26 6.6667V14.2C26 21.0667 21.7333 26.4 16 29.3333C10.2667 26.4 6 21.0667 6 14.2V6.6667L16 2.6667Z' }, { d: 'M12.6667 16L14.6667 18L19.3333 13.3333' }],
  'gestion-documentos':[{ d: 'M18.6667 2.66666H8.00008C7.29284 2.66666 6.61456 2.94761 6.11446 3.44771C5.61437 3.9478 5.33341 4.62609 5.33341 5.33333V26.6667C5.33341 27.3739 5.61437 28.0522 6.11446 28.5523C6.61456 29.0524 7.29284 29.3333 8.00008 29.3333H24.0001C24.7073 29.3333 25.3856 29.0524 25.8857 28.5523C26.3858 28.0522 26.6667 27.3739 26.6667 26.6667V10.6667L18.6667 2.66666Z' }, { d: 'M18.6667 2.66666V10.6667H26.6667' }, { d: 'M21.3333 17.3333H10.6667' }, { d: 'M21.3333 22.6667H10.6667' }, { d: 'M13.3333 12H10.6667' }],
  'facultad':          [{ d: 'M2.66675 29.3333H29.3334' }, { d: 'M4 29.3333V13.3333L16 4L28 13.3333V29.3333' }, { d: 'M10.6667 29.3333V20H21.3334V29.3333' }, { d: 'M10.6667 13.3333H13.3334' }, { d: 'M18.6667 13.3333H21.3334' }],
  'carrera':           [{ d: 'M28.56 14.5627C28.7987 14.4574 29.0012 14.2844 29.1426 14.0651C29.2839 13.8458 29.3578 13.5899 29.3551 13.329C29.3524 13.0681 29.2732 12.8138 29.1274 12.5975C28.9815 12.3811 28.7755 12.2123 28.5346 12.112L17.1066 6.90667C16.7592 6.74821 16.3818 6.6662 16 6.6662C15.6181 6.6662 15.2407 6.74821 14.8933 6.90667L3.46664 12.1067C3.22927 12.2106 3.02733 12.3815 2.88553 12.5984C2.74373 12.8153 2.66821 13.0689 2.66821 13.328C2.66821 13.5872 2.74373 13.8407 2.88553 14.0576C3.02733 14.2745 3.22927 14.4454 3.46664 14.5493L14.8933 19.76C15.2407 19.9185 15.6181 20.0005 16 20.0005C16.3818 20.0005 16.7592 19.9185 17.1066 19.76L28.56 14.5627Z' }, { d: 'M29.3333 13.3333V21.3333' }, { d: 'M8 16.6667V21.3333C8 22.3942 8.84286 23.4116 10.3431 24.1618C11.8434 24.9119 13.8783 25.3333 16 25.3333C18.1217 25.3333 20.1566 24.9119 21.6569 24.1618C23.1571 23.4116 24 22.3942 24 21.3333V16.6667' }],
  'materia':           [{ d: 'M4 6.66666H28' }, { d: 'M4 16H28' }, { d: 'M4 25.3333H28' }],
  'gestion-postulante':[{ d: 'M26.6667 28V25.3333C26.6667 23.9188 26.1048 22.5623 25.1046 21.5621C24.1044 20.5619 22.7479 20 21.3334 20H10.6667C9.25222 20 7.89567 20.5619 6.89547 21.5621C5.89528 22.5623 5.33337 23.9188 5.33337 25.3333V28' }, { d: 'M16 14.6667C18.9455 14.6667 21.3333 12.2789 21.3333 9.33333C21.3333 6.38781 18.9455 4 16 4C13.0545 4 10.6667 6.38781 10.6667 9.33333C10.6667 12.2789 13.0545 14.6667 16 14.6667Z' }],
  'auditoria': [{ d: 'M16 2.667C8.636 2.667 2.667 8.636 2.667 16S8.636 29.333 16 29.333 29.333 23.364 29.333 16 23.364 2.667 16 2.667z' }, { d: 'M16 10.667V16l5.333 2.667' }],
  'gestion-opciones': [{ d: 'M13.333 5.333h-8v8h8v-8z' }, { d: 'M26.667 5.333h-8v8h8v-8z' }, { d: 'M13.333 18.667h-8v8h8v-8z' }, { d: 'M26.667 18.667h-8v8h8v-8z' }],
  'config-institucion': [{ d: 'M25.333 28V6.667A2.667 2.667 0 0022.667 4H9.333A2.667 2.667 0 006.667 6.667V28' }, { d: 'M2.667 28h26.666' }, { d: 'M13.333 28v-6.667h5.334V28' }, { d: 'M12 10.667h1.333m5.334 0H20M12 16h1.333m5.334 0H20' }],
  'backup': [{ d: 'M10.667 25.333H8A5.333 5.333 0 018 14.667Q8 14 8.213 13.28A6.667 6.667 0 0121.333 16h1.334a4 4 0 010 8h-2.667' }, { d: 'M16 28V18.667' }, { d: 'M10.667 24l5.333-5.333L21.333 24' }],
  'niveles-academicos': [{ d: 'M16 4l12 6.667-12 6.666L4 10.667 16 4z' }, { d: 'M4 10.667L16 17.333l12-6.666' }, { d: 'M4 17.333L16 24l12-6.667' }],
  'area-conocimiento': [{ d: 'M16 8.337v17.333m0-17.333C14.443 7.303 12.328 6.667 10 6.667S5.557 7.303 4 8.337v17.333C5.557 24.697 7.672 24 10 24s4.443.697 6 1.67m0-17.333C17.557 7.303 19.672 6.667 22 6.667c2.329 0 4.443.697 6 1.67v17.333C26.443 24.697 24.328 24 22 24c-2.328 0-4.443.697-6 1.67' }]
};
const SVG_FALLBACK: Array<{ d: string }> = [{ d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' }, { d: 'M16 10.6667V16' }, { d: 'M16 21.3333H16.0133' }];
const QUICK_KEY = 'dashboard_admin_quick';

@Component({ selector: 'app-admin', standalone: true, imports: [CommonModule], templateUrl: './admin.html', styleUrls: ['./admin.scss'] })
export class AdminComponent implements OnInit {
  cards: DashCard[] = [];
  stats: DashStats = {};
  actividadReciente: any[] = [];
  cargandoStats = true;
  quickAccesos: string[] = [];
  editandoQuick = false;

  constructor(private router: Router, private http: HttpClient, private authService: AuthService, private logoService: LogoService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.construirCards();
    this.cargarStats();
    try { this.quickAccesos = JSON.parse(localStorage.getItem(QUICK_KEY) || '[]'); } catch { this.quickAccesos = []; }
    this.logoService.getNombre().subscribe(n => {});
  }

  private construirCards(): void {
    const modulo = this.authService.getModulo();
    if (!modulo?.opciones?.length) { this.cards = []; return; }
    this.cards = modulo.opciones.map((op: any) => {
      console.log('Rutas admin:', modulo.opciones.map((op: any) => op.ruta));
      const k = (op.ruta || '').replace(/^\//, '').split('/').pop() ?? '';
      return { titulo: op.nombre, descripcion: op.descripcion || '', ruta: k, svgPaths: SVG_MAP[k] ?? SVG_FALLBACK };
    });
  }

  private cargarStats(): void {
    const token = localStorage.getItem('token');
    const headers = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : undefined;
    this.http.get<any>(`${environment.apiUrl}/dashboard/admin`, headers ? { headers } : {}).subscribe({
      next: (d) => { this.stats = d; this.actividadReciente = d.actividadReciente || []; this.cargandoStats = false; this.cdr.detectChanges(); },
      error: () => { this.cargandoStats = false; }
    });
  }

  getActColor(a: string): string {
    return ({ INSERT: 'green', UPDATE: 'blue', DELETE: 'red', CREAR: 'green', ACTUALIZAR: 'blue', ELIMINAR: 'red', CAMBIAR_ESTADO: 'amber' } as any)[a?.toUpperCase()] ?? 'gray';
  }

  getActCount(accion: string): number {
    return this.actividadReciente.filter(i => i.accion?.toUpperCase() === accion.toUpperCase()).length;
  }

  getTopUsuarios(): { usuario: string; count: number }[] {
    const map: Record<string, number> = {};
    for (const item of this.actividadReciente) {
      if (item.usuario) map[item.usuario] = (map[item.usuario] || 0) + 1;
    }
    return Object.entries(map)
      .map(([usuario, count]) => ({ usuario, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 3);
  }

  getInitials(nombre: string): string {
    if (!nombre) return '?';
    const parts = nombre.trim().split(/\s+/);
    return parts.length >= 2 ? (parts[0][0] + parts[1][0]).toUpperCase() : nombre.slice(0, 2).toUpperCase();
  }

  getBarWidth(count: number, max: number): number {
    return max > 0 ? Math.round((count / max) * 100) : 0;
  }

  isQuickSelected(r: string) { return this.quickAccesos.includes(r); }
  toggleQuickItem(r: string) { this.quickAccesos = this.isQuickSelected(r) ? this.quickAccesos.filter(x => x !== r) : this.quickAccesos.length < 4 ? [...this.quickAccesos, r] : this.quickAccesos; localStorage.setItem(QUICK_KEY, JSON.stringify(this.quickAccesos)); }
  getQuickCards(): DashCard[] { return this.quickAccesos.map(r => this.cards.find(c => c.ruta === r)).filter((c): c is DashCard => !!c); }
  toggleConfigQuick() { this.editandoQuick = !this.editandoQuick; }
  navegarA(ruta: string) { this.router.navigate(['/admin', ruta]); }
}
