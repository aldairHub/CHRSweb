import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { ConvocatoriaService, Convocatoria, SolicitudDocente } from '../../services/convocatoria.service';

interface ConvocatoriaVM extends Convocatoria {
  expandida:           boolean;
  cargandoSolicitudes: boolean;
  solicitudes:         SolicitudDocente[];
  solicitudSeleccionada: SolicitudDocente | null;
}

@Component({
  selector: 'app-convocatorias-publicas',
  standalone: true,
  imports: [CommonModule, DatePipe],
  templateUrl: './convocatorias-publicas.html',
  styleUrls: ['./convocatorias-publicas.scss']
})
export class ConvocatoriasPublicasComponent implements OnInit {

  convocatorias: ConvocatoriaVM[] = [];
  cargando = true;

  // Modal "¿Primera vez o ya postulé?"
  mostrarModal          = false;
  solicitudParaPostular: SolicitudDocente | null = null;

  constructor(
    private convocatoriaService: ConvocatoriaService,
    private router: Router,
    private cdr:    ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.convocatoriaService.listarAbiertas().subscribe({
      next: (data) => {
        this.convocatorias = data.map(c => ({
          ...c,
          expandida:             false,
          cargandoSolicitudes:   false,
          solicitudes:           [],
          solicitudSeleccionada: null
        }));
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleConvocatoria(conv: ConvocatoriaVM): void {
    // Si ya estaba expandida, solo la colapsa
    if (conv.expandida) {
      conv.expandida             = false;
      conv.solicitudSeleccionada = null;
      return;
    }

    // Colapsar todas las demás
    this.convocatorias.forEach(c => {
      c.expandida             = false;
      c.solicitudSeleccionada = null;
    });

    conv.expandida = true;

    // Si ya cargó las solicitudes, no volver a pedir
    if (conv.solicitudes.length > 0) return;

    conv.cargandoSolicitudes = true;
    this.convocatoriaService.obtenerSolicitudes(conv.idConvocatoria).subscribe({
      next: (data) => {
        conv.solicitudes           = data;
        conv.cargandoSolicitudes   = false;
        // Si solo hay una, la preseleccionamos
        if (data.length === 1) conv.solicitudSeleccionada = data[0];
        this.cdr.detectChanges();
      },
      error: () => {
        conv.cargandoSolicitudes = false;
        this.cdr.detectChanges();
      }
    });
  }

  seleccionarSolicitud(conv: ConvocatoriaVM, s: SolicitudDocente): void {
    conv.solicitudSeleccionada =
      conv.solicitudSeleccionada?.idSolicitud === s.idSolicitud ? null : s;
  }

  abrirModal(conv: ConvocatoriaVM): void {
    if (!conv.solicitudSeleccionada) return;
    this.solicitudParaPostular = conv.solicitudSeleccionada;
    this.mostrarModal          = true;
  }

  cerrarModal(): void {
    this.mostrarModal          = false;
    this.solicitudParaPostular = null;
  }

  irARegistro(): void {
    if (!this.solicitudParaPostular) return;
    this.router.navigate(['/registro'], {
      queryParams: { idSolicitud: this.solicitudParaPostular.idSolicitud }
    });
  }

  irARepostulacion(): void {
    if (!this.solicitudParaPostular) return;
    this.router.navigate(['/repostulacion'], {
      queryParams: { idSolicitud: this.solicitudParaPostular.idSolicitud }
    });
  }

  volver():   void { this.router.navigate(['/']); }
  irALogin(): void { this.router.navigate(['/login']); }
}
