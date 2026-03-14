import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpParams } from '@angular/common/http';
import {RouterLink, RouterLinkActive} from '@angular/router';
import { ToastComponent } from '../../../component/toast.component';
import { ToastService } from '../../../services/toast.service';

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
  imports: [CommonModule, FormsModule, RouterLink, ToastComponent, RouterLinkActive],
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
    private http:  HttpClient,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService
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
        this.toast.error('Error', 'No se pudieron cargar los registros de auditoría.');
        this.cdr.detectChanges();
      }
    });
  }

  cargarStats(): void {
    const hoy = new Date().toISOString().slice(0, 10);
    this.http.get<any>(`${this.apiUrl}/estadisticas/login`).subscribe({
      next: (data) => {
        const tendencia = Array.isArray(data.tendenciaDiaria) ? data.tendenciaDiaria : [];
        const fila = tendencia.find((r: any) => r.dia === hoy);
        if (fila) {
          this.totalHoy    = Number(fila.total);
          this.exitososHoy = Number(fila.exitosos);
          this.fallidosHoy = Number(fila.fallidos);
        }
        this.totalElements = data.totalRegistros ?? this.totalElements;
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
