import { Component, OnInit,ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { LogoService } from '../../services/logo.service';
import { environment } from '../../../environments/environment';

interface DashCard {
  titulo:      string;
  descripcion: string;
  ruta:        string;
  svgPaths:    Array<{ d: string }>;
}

interface DashStats {
  usuariosActivos?:      number;
  postulantesPendientes?: number;
  convocatoriasAbiertas?: number;
  totalConvocatorias?:    number;
}

interface ActividadItem {
  descripcion: string;
  accion:      string;
  entidad:     string;
  usuario:     string;
  fecha:       string;
}

const SVG_MAP: Record<string, Array<{ d: string }>> = {
  'gestion-usuarios': [
    { d: 'M21.3334 28V25.3333C21.3334 23.9188 20.7715 22.5623 19.7713 21.5621C18.7711 20.5619 17.4146 20 16.0001 20H8.00008C6.58559 20 5.22904 20.5619 4.22885 21.5621C3.22865 22.5623 2.66675 23.9188 2.66675 25.3333V28' },
    { d: 'M12.0001 14.6667C14.9456 14.6667 17.3334 12.2789 17.3334 9.33333C17.3334 6.38781 14.9456 4 12.0001 4C9.05456 4 6.66675 6.38781 6.66675 9.33333C6.66675 12.2789 9.05456 14.6667 12.0001 14.6667Z' },
    { d: 'M29.3333 28V25.3333C29.3324 24.1516 28.9391 23.0037 28.2151 22.0698C27.4911 21.1358 26.4774 20.4688 25.3333 20.1733' },
    { d: 'M21.3333 4.17334C22.4805 4.46707 23.4973 5.13427 24.2234 6.06975C24.9496 7.00523 25.3437 8.15578 25.3437 9.34001C25.3437 10.5242 24.9496 11.6748 24.2234 12.6103C23.4973 13.5457 22.4805 14.2129 21.3333 14.5067' },
  ],
  'gestion-roles': [
    { d: 'M16 2.6667L26 6.6667V14.2C26 21.0667 21.7333 26.4 16 29.3333C10.2667 26.4 6 21.0667 6 14.2V6.6667L16 2.6667Z' },
    { d: 'M12.6667 16L14.6667 18L19.3333 13.3333' },
  ],
  'gestion-documentos': [
    { d: 'M18.6667 2.66666H8.00008C7.29284 2.66666 6.61456 2.94761 6.11446 3.44771C5.61437 3.9478 5.33341 4.62609 5.33341 5.33333V26.6667C5.33341 27.3739 5.61437 28.0522 6.11446 28.5523C6.61456 29.0524 7.29284 29.3333 8.00008 29.3333H24.0001C24.7073 29.3333 25.3856 29.0524 25.8857 28.5523C26.3858 28.0522 26.6667 27.3739 26.6667 26.6667V10.6667L18.6667 2.66666Z' },
    { d: 'M18.6667 2.66666V10.6667H26.6667' },
    { d: 'M21.3333 17.3333H10.6667' },
    { d: 'M21.3333 22.6667H10.6667' },
    { d: 'M13.3333 12H11.9999H10.6667' },
  ],
  'facultad': [
    { d: 'M2.66675 29.3333H29.3334' },
    { d: 'M4 29.3333V13.3333L16 4L28 13.3333V29.3333' },
    { d: 'M10.6667 29.3333V20H21.3334V29.3333' },
    { d: 'M10.6667 13.3333H13.3334' },
    { d: 'M18.6667 13.3333H21.3334' },
  ],
  'carrera': [
    { d: 'M28.56 14.5627C28.7987 14.4574 29.0012 14.2844 29.1426 14.0651C29.2839 13.8458 29.3578 13.5899 29.3551 13.329C29.3524 13.0681 29.2732 12.8138 29.1274 12.5975C28.9815 12.3811 28.7755 12.2123 28.5346 12.112L17.1066 6.90667C16.7592 6.74821 16.3818 6.6662 16 6.6662C15.6181 6.6662 15.2407 6.74821 14.8933 6.90667L3.46664 12.1067C3.22927 12.2106 3.02733 12.3815 2.88553 12.5984C2.74373 12.8153 2.66821 13.0689 2.66821 13.328C2.66821 13.5872 2.74373 13.8407 2.88553 14.0576C3.02733 14.2745 3.22927 14.4454 3.46664 14.5493L14.8933 19.76C15.2407 19.9185 15.6181 20.0005 16 20.0005C16.3818 20.0005 16.7592 19.9185 17.1066 19.76L28.56 14.5627Z' },
    { d: 'M29.3333 13.3333V21.3333' },
    { d: 'M8 16.6667V21.3333C8 22.3942 8.84286 23.4116 10.3431 24.1618C11.8434 24.9119 13.8783 25.3333 16 25.3333C18.1217 25.3333 20.1566 24.9119 21.6569 24.1618C23.1571 23.4116 24 22.3942 24 21.3333V16.6667' },
  ],
  'materia': [
    { d: 'M4 6.66666H28' },
    { d: 'M4 16H28' },
    { d: 'M4 25.3333H28' },
  ],
  'gestion-postulante': [
    { d: 'M26.6667 28V25.3333C26.6667 23.9188 26.1048 22.5623 25.1046 21.5621C24.1044 20.5619 22.7479 20 21.3334 20H10.6667C9.25222 20 7.89567 20.5619 6.89547 21.5621C5.89528 22.5623 5.33337 23.9188 5.33337 25.3333V28' },
    { d: 'M16 14.6667C18.9455 14.6667 21.3333 12.2789 21.3333 9.33333C21.3333 6.38781 18.9455 4 16 4C13.0545 4 10.6667 6.38781 10.6667 9.33333C10.6667 12.2789 13.0545 14.6667 16 14.6667Z' },
  ],
  'auditoria': [
    { d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' },
    { d: 'M16 8V16L21.3333 18.6667' },
  ],
  'gestion-opciones': [
    { d: 'M16 4L4 10V22L16 28L28 22V10L16 4Z' },
    { d: 'M16 16L4 10' },
    { d: 'M16 16V28' },
    { d: 'M16 16L28 10' },
  ],
  'config-institucion': [
    { d: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4' },
  ],
  'backup': [
    { d: 'M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12' },
  ],
  'niveles-academicos': [
    { d: 'M12 14l9-5-9-5-9 5 9 5z' },
    { d: 'M12 14l9-5-9-5-9 5 9 5zm0 0v6' },
    { d: 'M3 21l9-5 9 5' },
  ],
};

const SVG_FALLBACK: Array<{ d: string }> = [
  { d: 'M16 2.6667C8.6364 2.6667 2.6667 8.6364 2.6667 16C2.6667 23.3636 8.6364 29.3333 16 29.3333C23.3636 29.3333 29.3333 23.3636 29.3333 16C29.3333 8.6364 23.3636 2.6667 16 2.6667Z' },
  { d: 'M16 10.6667V16' },
  { d: 'M16 21.3333H16.0133' },
];

const QUICK_STORAGE_KEY = 'dashboard_quick_accesos';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin.html',
  styleUrls: ['./admin.scss']
})
export class AdminComponent implements OnInit {

  cards: DashCard[] = [];
  stats: DashStats = {};
  actividadReciente: ActividadItem[] = [];
  cargandoStats = true;

  // Acceso rápido
  quickAccesos: string[] = [];
  editandoQuick = false;

  constructor(
    private router:      Router,
    private http:        HttpClient,
    private authService: AuthService,private cdr: ChangeDetectorRef,
    private logoService: LogoService
  ) {}

  ngOnInit(): void {
    this.construirCards();
    this.cargarStats();
    this.cargarQuickAccesos();
  }

  // ── Cards módulos ──────────────────────────────────────────
  private construirCards(): void {
    const modulo = this.authService.getModulo();
    if (!modulo?.opciones?.length) { this.cards = []; return; }

    this.cards = modulo.opciones.map(op => {
      const rutaKey = (op.ruta || '').replace(/^\//, '').split('/').pop() ?? '';
      return {
        titulo:      op.nombre,
        descripcion: op.descripcion || '',
        ruta:        rutaKey,
        svgPaths:    SVG_MAP[rutaKey] ?? SVG_FALLBACK,
      };
    });
  }

  // ── Stats desde backend ────────────────────────────────────
  private cargarStats(): void {
    const token = localStorage.getItem('token');
    const headers = token ? new HttpHeaders({ Authorization: `Bearer ${token}` }) : undefined;
    const url = `${environment.apiUrl}/dashboard/admin-stats`;

    this.http.get<any>(url, headers ? { headers } : {}).subscribe({
      next: (data) => {
        this.stats = {
          usuariosActivos:       data.usuariosActivos,
          postulantesPendientes: data.postulantesPendientes,
          convocatoriasAbiertas: data.convocatoriasAbiertas,
          totalConvocatorias:    data.totalConvocatorias,
        };
        this.actividadReciente = data.actividadReciente || [];
        this.cargandoStats = false;
      },
      error: () => {
        // Si falla (no autenticado o backend caído), mostrar vacío
        this.cargandoStats = false;
      }
    });
  }

  // ── Color de punto según acción ────────────────────────────
  getActColor(accion: string): string {
    const map: Record<string, string> = {
      CREAR:           'green',
      ACTUALIZAR:      'blue',
      ELIMINAR:        'red',
      CAMBIAR_ESTADO:  'amber',
      SUBIR_DOCUMENTO: 'blue',
      GENERAR_REPORTE: 'purple',
    };
    return map[accion?.toUpperCase()] ?? 'gray';
  }

  // ── Acceso rápido ──────────────────────────────────────────
  private cargarQuickAccesos(): void {
    try {
      const raw = localStorage.getItem(QUICK_STORAGE_KEY);
      this.quickAccesos = raw ? JSON.parse(raw) : [];
    } catch {
      this.quickAccesos = [];  this.cdr.detectChanges();
    }
  }

  isQuickSelected(ruta: string): boolean {
    return this.quickAccesos.includes(ruta);
  }

  toggleQuickItem(ruta: string): void {
    if (this.isQuickSelected(ruta)) {
      this.quickAccesos = this.quickAccesos.filter(r => r !== ruta);
    } else if (this.quickAccesos.length < 4) {
      this.quickAccesos = [...this.quickAccesos, ruta];
    }
    localStorage.setItem(QUICK_STORAGE_KEY, JSON.stringify(this.quickAccesos));
  }

  getQuickCards(): DashCard[] {
    return this.quickAccesos
      .map(ruta => this.cards.find(c => c.ruta === ruta))
      .filter((c): c is DashCard => !!c);
  }

  toggleConfigQuick(): void {
    this.editandoQuick = !this.editandoQuick;
  }

  // ── Navegación ─────────────────────────────────────────────
  navegarA(ruta: string): void {
    this.router.navigate(['/admin', ruta]);
  }
}
