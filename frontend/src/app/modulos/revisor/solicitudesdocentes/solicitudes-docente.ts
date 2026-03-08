// solicitudes-docente.ts
import { Component, OnInit,ChangeDetectorRef  } from '@angular/core';
import { CommonModule, DecimalPipe, TitleCasePipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { NavbarComponent } from '../../../component/navbar';
import { FooterComponent } from '../../../component/footer.component';
import { environment } from '../../../../environments/environment';

export interface SolicitudDocenteResponseDTO {
  idSolicitud?:               number;
  idAutoridad?:               number;
  nombreAutoridad?:           string;
  idCarrera?:                 number;
  nombreCarrera?:             string;
  modalidadCarrera?:          string;
  idFacultad?:                number;
  nombreFacultad?:            string;
  idMateria?:                 number;
  nombreMateria?:             string;
  nivelMateria?:              string;
  idArea?:                    number;
  nombreArea?:                string;
  fechaSolicitud?:            string;
  estadoSolicitud?:           string;
  justificacion?:             string;
  cantidadDocentes?:          number;
  nivelAcademico?:            string;
  experienciaProfesionalMin?: number;
  experienciaDocenteMin?:     number;
  observaciones?:             string;
}

@Component({
  selector: 'app-solicitudes-docente',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent, FooterComponent,
    DecimalPipe, TitleCasePipe, DatePipe],
  templateUrl: './solicitudes-docente.html',
  styleUrls:  ['./solicitudes-docente.scss']
})
export class SolicitudesDocenteComponent implements OnInit {

  private readonly API = `${environment.apiUrl}/solicitudes-docente`;

  // ── data ─────────────────────────────────────────────────────
  cargando = false;
  solicitudes:          SolicitudDocenteResponseDTO[] = [];
  solicitudesFiltradas: SolicitudDocenteResponseDTO[] = [];

  // ── filtros ───────────────────────────────────────────────────
  busqueda     = '';
  filtroEstado = '';

  // ── paginación ────────────────────────────────────────────────
  paginaActual   = 1;
  itemsPorPagina = 10;

  // ── modal detalle ─────────────────────────────────────────────
  solicitudDetalle: SolicitudDocenteResponseDTO | null = null;

  // ── modal confirmar acción ────────────────────────────────────
  modalConfirm        = false;
  solicitudParaAccion: SolicitudDocenteResponseDTO | null = null;
  accionPendiente     = '';          // 'aprobada' | 'rechazada'
  observacionesAccion = '';
  procesando          = false;

  constructor(private http: HttpClient,   private cdr: ChangeDetectorRef) {}

  ngOnInit(): void { this.cargar(); }

  // ── carga ──────────────────────────────────────────────────────
  cargar(): void { this.cdr.detectChanges();
    this.cargando = true;
    this.http.get<SolicitudDocenteResponseDTO[]>(this.API).subscribe({
      next: data => { this.solicitudes = data; this.filtrar(); this.cargando = false; this.cdr.detectChanges(); },
      error: err  => { this.cargando = false; }

    })
    ;
    this.cdr.detectChanges();

  }

  // ── filtro ────────────────────────────────────────────────────
  filtrar(): void {
    const q = this.busqueda.toLowerCase().trim();
    this.solicitudesFiltradas = this.solicitudes.filter(s => {
      const matchQ = !q || [s.nombreMateria, s.nombreCarrera, s.nombreArea]
        .some(v => v?.toLowerCase().includes(q));
      const matchE = !this.filtroEstado ||
        s.estadoSolicitud?.toLowerCase() === this.filtroEstado;
      return matchQ && matchE;
    });
    this.paginaActual = 1;
  }

  // ── stats ─────────────────────────────────────────────────────
  contarEstado(e: string): number {
    return this.solicitudes.filter(s => s.estadoSolicitud?.toLowerCase() === e).length;
  }

  // ── paginación ────────────────────────────────────────────────
  get totalPaginas(): number { return Math.ceil(this.solicitudesFiltradas.length / this.itemsPorPagina); }
  get inicio(): number       { return (this.paginaActual - 1) * this.itemsPorPagina; }
  get fin(): number          { return Math.min(this.inicio + this.itemsPorPagina, this.solicitudesFiltradas.length); }
  get paginadas():  SolicitudDocenteResponseDTO[] { return this.solicitudesFiltradas.slice(this.inicio, this.fin); }
  get paginas():    number[] { return Array.from({ length: this.totalPaginas }, (_, i) => i + 1); }
  cambiarPagina(p: number): void { if (p >= 1 && p <= this.totalPaginas) this.paginaActual = p; }

  // ── helpers CSS ───────────────────────────────────────────────
  estadoClass(e?: string): string {
    switch (e?.toLowerCase()) {
      case 'aprobada':  return 'success';
      case 'rechazada': return 'danger';
      default:          return 'warning';
    }
  }

  // ── modal detalle ─────────────────────────────────────────────
  verDetalle(s: SolicitudDocenteResponseDTO): void { this.solicitudDetalle = s; }
  cerrarDetalle(): void                            { this.solicitudDetalle = null; }

  // ── acción rápida (abre modal de confirmación) ─────────────────
  accionRapida(s: SolicitudDocenteResponseDTO, accion: string): void {
    this.solicitudParaAccion  = s;
    this.accionPendiente      = accion;
    this.observacionesAccion  = '';
    this.modalConfirm         = true;
    this.cdr.detectChanges();
  }

  cancelarConfirm(): void {
    this.modalConfirm        = false;
    this.solicitudParaAccion = null;
    this.accionPendiente     = '';
    this.cdr.detectChanges();

  }

  confirmarAccion(): void {
    if (!this.solicitudParaAccion?.idSolicitud || !this.accionPendiente) return;
    this.procesando = true;

    this.cdr.detectChanges();
    const body = {
      nuevoEstado:   this.accionPendiente,
      observaciones: this.observacionesAccion
    };

    this.http.patch<SolicitudDocenteResponseDTO>(
      `${this.API}/${this.solicitudParaAccion.idSolicitud}/estado`, body
    ).subscribe({
      next: updated => {
        // Actualizar en memoria
        const idx = this.solicitudes.findIndex(s => s.idSolicitud === updated.idSolicitud);
        if (idx !== -1) this.solicitudes[idx] = updated;
        this.filtrar();
        this.procesando = false;
        this.cdr.detectChanges();
        this.cancelarConfirm();
      },
      error: err => {
        this.cargando = false;
        this.procesando = false;
      }
    });
  }

  // ── PDF ───────────────────────────────────────────────────────
  abrirPDF(id: number): void {
    window.open(`${this.API}/${id}/reporte-pdf`, '_blank');
  }
}
