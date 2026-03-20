// solicitudes-docente.ts
import { Router } from '@angular/router';
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DecimalPipe, TitleCasePipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../services/toast.service';
import { ToastComponent } from '../../../component/toast.component';

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

export interface RequisitoPrepostulacionDTO {
  idRequisito: number;
  nombre: string;
  descripcion: string | null;
  orden: number;
}

// ── Reporte ───────────────────────────────────────────────────────────────────
interface ReporteSolicitudConfig {
  titulo: string;
  subtitulo: string;
  formato: 'PDF' | 'EXCEL';
  orientacion: 'VERTICAL' | 'HORIZONTAL';
  desde: string;
  hasta: string;
  estado: string;
  facultad: string;
  incluirPortada: boolean;
  incluirKpis: boolean;
  incluirDetalle: boolean;
  incluirGraficoEstados: boolean;
  incluirGraficoCarreras: boolean;
  incluirGraficoAreas: boolean;
  incluirGraficoTemporal: boolean;
  colorPrimario: string;
  mostrarNumeroPagina: boolean;
  mostrarFechaGeneracion: boolean;
  excelCongelarEncabezado: boolean;
  excelFiltrosAutomaticos: boolean;
}

@Component({
  selector: 'app-solicitudes-docente',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe, TitleCasePipe, DatePipe, ToastComponent],
  templateUrl: './solicitudes-docente.html',
  styleUrls:  ['./solicitudes-docente.scss']
})
export class SolicitudesDocenteComponent implements OnInit {

  private readonly API = `${environment.apiUrl}/solicitudes-docente`;

  // ── data ──────────────────────────────────────────────────────
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
  accionPendiente     = '';
  observacionesAccion = '';
  procesando          = false;

  // ── modal requisitos ──────────────────────────────────────────
  modalRequisitos                      = false;
  solicitudParaRequisitos: SolicitudDocenteResponseDTO | null = null;
  requisitosDetalleList: RequisitoPrepostulacionDTO[]  = [];
  cargandoRequisitos                   = false;
  nuevoReqNombre                       = '';
  nuevoReqDesc                         = '';
  nuevoReqOrden                        = 0;
  guardandoRequisito                   = false;
  editandoRequisito: RequisitoPrepostulacionDTO | null = null;

  // ── Modal Reporte ─────────────────────────────────────────────
  showReporteModal    = false;
  reporteGenerando    = false;
  reporteTabActiva: 'filtros' | 'secciones' | 'visual' | 'exportar' = 'filtros';
  reporteConfig: ReporteSolicitudConfig = {
    titulo: 'Reporte de Solicitudes Docentes',
    subtitulo: '',
    formato: 'PDF',
    orientacion: 'VERTICAL',
    desde: '',
    hasta: '',
    estado: '',
    facultad: '',
    incluirPortada: true,
    incluirKpis: true,
    incluirDetalle: true,
    incluirGraficoEstados: true,
    incluirGraficoCarreras: true,
    incluirGraficoAreas: true,
    incluirGraficoTemporal: true,
    colorPrimario: '#2563EB',
    mostrarNumeroPagina: true,
    mostrarFechaGeneracion: true,
    excelCongelarEncabezado: true,
    excelFiltrosAutomaticos: true,
  };
  private readonly reporteApiUrl = 'http://localhost:8080/api/admin/solicitudes-docentes/reporte/generar';

  constructor(
    private http:  HttpClient,
    private cdr:   ChangeDetectorRef,
    private toast: ToastService
    ,
    private router: Router) {}

  ngOnInit(): void { this.cargar(); }

  // ── carga ─────────────────────────────────────────────────────
  cargar(): void {
    this.cargando = true;
    this.cdr.detectChanges();
    this.http.get<SolicitudDocenteResponseDTO[]>(this.API).subscribe({
      next: data => {
        this.solicitudes = data;
        this.filtrar();
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.toast.error('Error', 'No se pudieron cargar las solicitudes.');
        this.cdr.detectChanges();
      }
    });
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
  get paginadas(): SolicitudDocenteResponseDTO[] { return this.solicitudesFiltradas.slice(this.inicio, this.fin); }
  get paginas():   number[]  { return Array.from({ length: this.totalPaginas }, (_, i) => i + 1); }
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

  // ── acción rápida ─────────────────────────────────────────────
  accionRapida(s: SolicitudDocenteResponseDTO, accion: string): void {
    this.solicitudParaAccion = s;
    this.accionPendiente     = accion;
    this.observacionesAccion = '';
    this.modalConfirm        = true;
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
        const idx = this.solicitudes.findIndex(s => s.idSolicitud === updated.idSolicitud);
        if (idx !== -1) this.solicitudes[idx] = updated;
        this.filtrar();
        this.procesando = false;
        this.cdr.detectChanges();
        const accionLabel = this.accionPendiente === 'aprobada' ? 'aprobada' : 'rechazada';
        const materia     = updated.nombreMateria ?? 'la solicitud';
        this.toast.success(`Solicitud ${accionLabel}`, `${materia} ha sido ${accionLabel} correctamente.`);
        this.cancelarConfirm();
      },
      error: err => {
        this.procesando = false;
        this.toast.error('Error', err?.error?.mensaje || 'No se pudo cambiar el estado.');
        this.cdr.detectChanges();
      }
    });
  }

  // ── PDF ───────────────────────────────────────────────────────
  abrirPDF(id: number): void {
    window.open(`${this.API}/${id}/reporte-pdf`, '_blank');
  }

  // ── REQUISITOS DE PREPOSTULACIÓN ──────────────────────────────

  abrirRequisitos(s: SolicitudDocenteResponseDTO): void {
    this.solicitudParaRequisitos = s;
    this.modalRequisitos         = true;
    this.nuevoReqNombre          = '';
    this.nuevoReqDesc            = '';
    this.nuevoReqOrden           = 0;
    this.editandoRequisito       = null;
    this.cargarRequisitos(s.idSolicitud!);
  }

  cerrarRequisitos(): void {
    this.modalRequisitos         = false;
    this.solicitudParaRequisitos = null;
    this.requisitosDetalleList   = [];
    this.editandoRequisito       = null;
    this.cdr.detectChanges();
  }

  cargarRequisitos(idSolicitud: number): void {
    this.cargandoRequisitos = true;
    this.http.get<RequisitoPrepostulacionDTO[]>(
      `http://localhost:8080/api/admin/solicitudes/${idSolicitud}/requisitos`
    ).subscribe({
      next: data => {
        this.requisitosDetalleList = data;
        this.cargandoRequisitos    = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargandoRequisitos = false;
        this.toast.error('Error', 'No se pudieron cargar los requisitos.');
        this.cdr.detectChanges();
      }
    });
  }

  agregarRequisito(): void {
    if (!this.nuevoReqNombre.trim() || !this.solicitudParaRequisitos?.idSolicitud) return;
    this.guardandoRequisito = true;
    const body = {
      nombre:      this.nuevoReqNombre.trim(),
      descripcion: this.nuevoReqDesc.trim() || null,
      orden:       this.nuevoReqOrden
    };
    this.http.post<RequisitoPrepostulacionDTO>(
      `http://localhost:8080/api/admin/solicitudes/${this.solicitudParaRequisitos.idSolicitud}/requisitos`,
      body
    ).subscribe({
      next: nuevo => {
        this.requisitosDetalleList = [...this.requisitosDetalleList, nuevo];
        this.nuevoReqNombre        = '';
        this.nuevoReqDesc          = '';
        this.nuevoReqOrden         = 0;
        this.guardandoRequisito    = false;
        this.toast.success('Requisito agregado', nuevo.nombre);
        this.cdr.detectChanges();
      },
      error: () => {
        this.guardandoRequisito = false;
        this.toast.error('Error', 'No se pudo agregar el requisito.');
        this.cdr.detectChanges();
      }
    });
  }

  iniciarEdicionReq(r: RequisitoPrepostulacionDTO): void {
    this.editandoRequisito = { ...r };
    this.cdr.detectChanges();
  }

  cancelarEdicionReq(): void {
    this.editandoRequisito = null;
    this.cdr.detectChanges();
  }

  guardarEdicionReq(): void {
    if (!this.editandoRequisito || !this.editandoRequisito.nombre.trim()) return;
    this.guardandoRequisito = true;
    const body = {
      nombre:      this.editandoRequisito.nombre.trim(),
      descripcion: this.editandoRequisito.descripcion,
      orden:       this.editandoRequisito.orden
    };
    this.http.put(
      `http://localhost:8080/api/admin/solicitudes/requisitos/${this.editandoRequisito.idRequisito}`,
      body
    ).subscribe({
      next: () => {
        const idx = this.requisitosDetalleList.findIndex(r => r.idRequisito === this.editandoRequisito!.idRequisito);
        if (idx !== -1) this.requisitosDetalleList[idx] = { ...this.editandoRequisito! };
        this.editandoRequisito  = null;
        this.guardandoRequisito = false;
        this.toast.success('Requisito actualizado', '');
        this.cdr.detectChanges();
      },
      error: () => {
        this.guardandoRequisito = false;
        this.toast.error('Error', 'No se pudo actualizar el requisito.');
        this.cdr.detectChanges();
      }
    });
  }

  eliminarRequisito(r: RequisitoPrepostulacionDTO): void {
    if (!confirm(`¿Eliminar el requisito "${r.nombre}"?`)) return;
    this.http.delete(
      `http://localhost:8080/api/admin/solicitudes/requisitos/${r.idRequisito}`
    ).subscribe({
      next: () => {
        this.requisitosDetalleList = this.requisitosDetalleList.filter(x => x.idRequisito !== r.idRequisito);
        this.toast.success('Eliminado', r.nombre);
        this.cdr.detectChanges();
      },
      error: () => {
        this.toast.error('Error', 'No se pudo eliminar el requisito.');
        this.cdr.detectChanges();
      }
    });
  }

  // ── REPORTE ───────────────────────────────────────────────────

  exportarReporte(): void {
    this.router.navigate(['/revisor/estadisticas-solicitudes']);
  }

  cerrarReporteModal(): void { this.showReporteModal = false; }

  cambiarReporteTab(tab: 'filtros' | 'secciones' | 'visual' | 'exportar'): void {
    this.reporteTabActiva = tab;
  }

  get reporteSeccionesActivas(): number {
    const s = this.reporteConfig;
    return [s.incluirPortada, s.incluirKpis, s.incluirDetalle,
      s.incluirGraficoEstados, s.incluirGraficoCarreras,
      s.incluirGraficoAreas, s.incluirGraficoTemporal]
      .filter(Boolean).length;
  }

  generarReporte(): void {
    this.reporteGenerando = true;
    this.http.post(this.reporteApiUrl, this.reporteConfig, {
      responseType: 'blob', observe: 'response'
    }).subscribe({
      next: (response) => {
        const blob  = response.body!;
        const cd    = response.headers.get('content-disposition') || '';
        const match = cd.match(/filename="?([^";\n]+)"?/);
        const nombre = match ? match[1]
          : `reporte_solicitudes.${this.reporteConfig.formato === 'EXCEL' ? 'xlsx' : 'pdf'}`;
        const url = window.URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href = url; a.download = nombre; a.click();
        window.URL.revokeObjectURL(url);
        this.reporteGenerando = false;
        this.cerrarReporteModal();
        this.toast.success('Reporte generado correctamente');
        this.cdr.detectChanges();
      },
      error: () => {
        this.reporteGenerando = false;
        this.toast.error('Error al generar el reporte');
        this.cdr.detectChanges();
      }
    });
  }
}
