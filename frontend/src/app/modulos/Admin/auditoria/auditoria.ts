import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import { NavbarComponent } from '../../../component/navbar';

interface AudLoginApp {
  idAud:      number;
  fecha:      string;
  usuarioApp: string;
  usuarioBd:  string | null;
  resultado:  'SUCCESS' | 'FAIL';
  motivo:     string | null;
  ipCliente:  string | null;
  userAgent:  string | null;
}

interface Page<T> {
  content:          T[];
  totalElements:    number;
  totalPages:       number;
  number:           number;
  size:             number;
}

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './auditoria.html',
  styleUrls: ['./auditoria.scss']
})
export class AuditoriaComponent implements OnInit {

  private apiUrl = 'http://localhost:8080/api/admin/auditoria';

  // ─── Datos ─────────────────────────────────────────────────
  registros:     AudLoginApp[] = [];
  totalElements  = 0;
  totalPages     = 0;
  currentPage    = 0;
  pageSize       = 20;
  isLoading      = false;

  // ─── Stats ─────────────────────────────────────────────────
  totalHoy       = 0;
  exitososHoy    = 0;
  fallidosHoy    = 0;

  // ─── Filtros ───────────────────────────────────────────────
  filtroUsuario  = '';
  filtroResultado = '';
  filtroDesde    = '';
  filtroHasta    = '';

  // ─── Detalle ───────────────────────────────────────────────
  registroDetalle: AudLoginApp | null = null;

  constructor(
    private http: HttpClient,
    private cdr:  ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.cargar();
    this.cargarStats();
  }

  cargar(page = 0): void {
    this.isLoading = true;
    let params = new HttpParams()
      .set('page',  page.toString())
      .set('size',  this.pageSize.toString());

    if (this.filtroUsuario)   params = params.set('usuarioApp', this.filtroUsuario);
    if (this.filtroResultado) params = params.set('resultado',  this.filtroResultado);
    if (this.filtroDesde)     params = params.set('desde',      this.filtroDesde);
    if (this.filtroHasta)     params = params.set('hasta',      this.filtroHasta);

    this.http.get<Page<AudLoginApp>>(`${this.apiUrl}/login`, { params }).subscribe({
      next: (data) => {
        this.registros      = data.content;
        this.totalElements  = data.totalElements;
        this.totalPages     = data.totalPages;
        this.currentPage    = data.number;
        this.isLoading      = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  cargarStats(): void {
    this.http.get<any[]>(`${this.apiUrl}/login/stats`).subscribe({
      next: (rows) => {
        const hoy = new Date().toISOString().slice(0, 10);
        const fila = rows.find(r => r[0] === hoy);
        if (fila) {
          this.totalHoy    = Number(fila[1]);
          this.exitososHoy = Number(fila[2]);
          this.fallidosHoy = Number(fila[3]);
        }
        this.cdr.detectChanges();
      }
    });
  }

  aplicarFiltros(): void { this.cargar(0); }

  limpiarFiltros(): void {
    this.filtroUsuario   = '';
    this.filtroResultado = '';
    this.filtroDesde     = '';
    this.filtroHasta     = '';
    this.cargar(0);
  }

  irPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.cargar(p);
  }

  verDetalle(r: AudLoginApp): void  { this.registroDetalle = r; }
  cerrarDetalle(): void             { this.registroDetalle = null; }

  formatFecha(fecha: string): string {
    return new Date(fecha).toLocaleString('es-EC', {
      day:    '2-digit', month: '2-digit', year: 'numeric',
      hour:   '2-digit', minute: '2-digit', second: '2-digit'
    });
  }

  formatMotivo(motivo: string | null): string {
    const map: Record<string, string> = {
      USER_NOT_FOUND:  'Usuario no encontrado',
      USER_DISABLED:   'Usuario desactivado',
      BAD_CREDENTIALS: 'Contraseña incorrecta',
      ERROR:           'Error interno',
      LOGOUT:          'Cierre de sesión'
    };
    return motivo ? (map[motivo] ?? motivo) : '—';
  }

  get pages(): number[] {
    const total = this.totalPages;
    const cur   = this.currentPage;
    const delta = 2;
    const range: number[] = [];
    for (let i = Math.max(0, cur - delta); i <= Math.min(total - 1, cur + delta); i++) {
      range.push(i);
    }
    return range;
  }
}
