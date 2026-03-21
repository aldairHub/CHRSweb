import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';

export interface PostulanteLista {
  idPostulacion:       number;
  idPostulante:        number;
  identificacion:      string;
  nombresPostulante:   string;
  apellidosPostulante: string;
  correoPostulante:    string;
  estadoPostulacion:   string;
  nombreMateria:       string;
  tieneDocumentos:     boolean; // ← tuyo
}

export interface ConvocatoriaResumen {
  id_convocatoria:     number;
  titulo:              string;
  estado_convocatoria: string;
  fecha_inicio:        string;
  fecha_fin:           string;
}

export interface SolicitudResumen {
  id_solicitud:       number;
  nombre_materia:     string;
  nombre_carrera:     string;
  nombre_facultad:    string;
  cantidad_docentes:  number;
  nivel_academico:    string;
  total_postulantes:  number;
}

@Component({
  selector: 'app-postulantes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './postulantes.html',
  styleUrls: ['./postulantes.scss']
})
export class PostulantesComponent implements OnInit {

  // ── Wizard steps ──
  paso = 1;

  // ── Paso 1 ──
  cargandoConvocatorias = false;
  convocatorias: ConvocatoriaResumen[] = [];
  convocatoriaSeleccionada: ConvocatoriaResumen | null = null;

  // ── Paso 2 ──
  cargandoSolicitudes = false;
  solicitudes: SolicitudResumen[] = [];
  solicitudSeleccionada: SolicitudResumen | null = null;

  // ── Paso 3 ──
  cargando = false;
  postulantes:      PostulanteLista[] = [];
  postulantesTotal: PostulanteLista[] = [];
  filtro = { cedula: '', apellido: '' };
  errorCarga: string | null = null;

  // ── Modales ──
  mostrarModal         = false;
  correoSeleccionado   = '';
  mostrarModalSinDocs  = false; // ← tuyo
  postulanteSinDocs    = '';    // ← tuyo

  constructor(
    private router: Router,
    private route:  ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.cargarConvocatorias();
    // Cuando se vuelve desde documentos.ts con ?idSolicitud=X
    // saltamos directo al paso 3 de la solicitud correspondiente
    this.route.queryParams.subscribe(qp => {
      const idSolicitud = qp['idSolicitud'] ? Number(qp['idSolicitud']) : null;
      if (idSolicitud && this.paso === 1) {
        this.cargarYSaltarASolicitud(idSolicitud);
      }
    });
  }

  // ── Paso 1: Convocatorias ──

  cargarConvocatorias(): void {
    this.cargandoConvocatorias = true;
    const headers = this.getHeaders();
    this.http.get<ConvocatoriaResumen[]>('/api/postulaciones/evaluador/convocatorias', { headers })
      .subscribe({
        next: (data) => {
          this.convocatorias = data;
          this.cargandoConvocatorias = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.convocatorias = [];
          this.cargandoConvocatorias = false;
          this.cdr.detectChanges();
        }
      });
  }

  seleccionarConvocatoria(conv: ConvocatoriaResumen): void {
    this.convocatoriaSeleccionada = conv;
    this.solicitudSeleccionada = null;
    this.solicitudes = [];
    this.paso = 2;
    this.cargarSolicitudes(conv.id_convocatoria);
    this.cdr.detectChanges();
  }

  // ── Paso 2: Solicitudes ──

  cargarSolicitudes(idConvocatoria: number): void {
    this.cargandoSolicitudes = true;
    const headers = this.getHeaders();
    this.http.get<SolicitudResumen[]>(
      `/api/postulaciones/evaluador/solicitudes?idConvocatoria=${idConvocatoria}`, { headers }
    ).subscribe({
      next: (data) => {
        this.solicitudes = data;
        this.cargandoSolicitudes = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.solicitudes = [];
        this.cargandoSolicitudes = false;
        this.cdr.detectChanges();
      }
    });
  }

  seleccionarSolicitud(sol: SolicitudResumen): void {
    this.solicitudSeleccionada = sol;
    this.postulantes = [];
    this.postulantesTotal = [];
    this.filtro = { cedula: '', apellido: '' };
    this.paso = 3;
    this.cargarPostulantes(sol.id_solicitud);
    this.cdr.detectChanges();
  }

  // ── Paso 3: Postulantes ──

  cargarPostulantes(idSolicitud: number): void {
    this.cargando = true;
    this.errorCarga = null;
    const headers = this.getHeaders();
    this.http.get<PostulanteLista[]>(
      `/api/postulaciones/evaluador/lista?idSolicitud=${idSolicitud}`, { headers }
    ).subscribe({
      next: (data) => {
        this.postulantesTotal = data;
        this.postulantes = data;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorCarga = 'No se pudo cargar la lista de postulantes.';
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  buscar(): void {
    const cedula   = this.filtro.cedula.toLowerCase().trim();
    const apellido = this.filtro.apellido.toLowerCase().trim();
    this.postulantes = this.postulantesTotal.filter(p => {
      const matchCedula   = !cedula   || p.identificacion.toLowerCase().includes(cedula);
      const matchApellido = !apellido || p.apellidosPostulante.toLowerCase().includes(apellido);
      return matchCedula && matchApellido;
    });
    this.cdr.detectChanges();
  }

  limpiarFiltros(): void {
    this.filtro = { cedula: '', apellido: '' };
    this.postulantes = this.postulantesTotal;
    this.cdr.detectChanges();
  }

  verDocumentos(p: PostulanteLista): void {
    if (!p.tieneDocumentos) {
      this.postulanteSinDocs = `${p.apellidosPostulante} ${p.nombresPostulante}`;
      this.mostrarModalSinDocs = true;
      this.cdr.detectChanges();
      return;
    }
    // Pasar idSolicitud como queryParam para que documentos.ts pueda volver al paso 3
    const idSolicitud = this.solicitudSeleccionada?.id_solicitud ?? null;
    this.router.navigate(
      ['/evaluador/documentos/' + p.idPostulacion],
      idSolicitud ? { queryParams: { idSolicitud } } : {}
    );
  }

  // ── Navegación wizard ──

  volverAPaso1(): void {
    this.paso = 1;
    this.convocatoriaSeleccionada = null;
    this.solicitudSeleccionada = null;
    this.solicitudes = [];
    this.postulantes = [];
    this.cdr.detectChanges();
  }

  volverAPaso2(): void {
    this.paso = 2;
    this.solicitudSeleccionada = null;
    this.postulantes = [];
    this.cdr.detectChanges();
  }

  volver(): void {
    this.router.navigate(['/evaluador']);
  }

  // ── Modales ──

  abrirModalExito(correo: string = ''): void {
    this.correoSeleccionado = correo;
    this.mostrarModal = true;
    this.cdr.detectChanges();
  }

  cerrarModal(): void {
    this.mostrarModal = false;
    this.cdr.detectChanges();
  }

  cerrarModalSinDocs(): void {
    this.mostrarModalSinDocs = false;
    this.cdr.detectChanges();
  }

  // ── Helpers ──

  /** Cuando se vuelve desde documentos con ?idSolicitud, carga y salta al paso 3 */
  private cargarYSaltarASolicitud(idSolicitud: number): void {
    const headers = this.getHeaders();
    // Cargar la lista de postulantes de esa solicitud directamente
    this.cargando = true;
    this.errorCarga = null;
    this.http.get<PostulanteLista[]>(
      `/api/postulaciones/evaluador/lista?idSolicitud=${idSolicitud}`, { headers }
    ).subscribe({
      next: (data) => {
        this.postulantesTotal = data;
        this.postulantes      = data;
        this.solicitudSeleccionada = { id_solicitud: idSolicitud } as any;
        this.paso    = 3;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        // Si falla, se queda en paso 1 normalmente
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return token ? new HttpHeaders({ Authorization: 'Bearer ' + token }) : new HttpHeaders();
  }

  estadoConvClass(estado: string): string {
    switch ((estado || '').toLowerCase()) {
      case 'abierta':   return 'badge-abierta';
      case 'cerrada':   return 'badge-cerrada';
      case 'cancelada': return 'badge-cerrada';
      default:          return 'badge-cerrada';
    }
  }

  estadoClass(estado: string): string {
    switch ((estado || '').toLowerCase()) {
      case 'pendiente':   return 'status-pendiente';
      case 'en_revision': return 'status-revision';
      case 'aprobada':    return 'status-aprobado';
      case 'rechazada':   return 'status-rechazado';
      default:            return 'status-pendiente';
    }
  }
}
