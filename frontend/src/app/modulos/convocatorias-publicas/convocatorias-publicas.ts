import { Component, OnInit, OnDestroy, ChangeDetectorRef, HostListener, ElementRef } from '@angular/core';
import { AsyncPipe, CommonModule, DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { ConvocatoriaService, Convocatoria, SolicitudDocente } from '../../services/convocatoria.service';
import { LogoService } from '../../services/logo.service';

interface ConvocatoriaVM extends Convocatoria {
  expandida:             boolean;
  cargandoSolicitudes:   boolean;
  solicitudes:           SolicitudDocente[];
  solicitudSeleccionada: SolicitudDocente | null;
}

@Component({
  selector: 'app-convocatorias-publicas',
  standalone: true,
  imports: [CommonModule, DatePipe, AsyncPipe],
  templateUrl: './convocatorias-publicas.html',
  styleUrls: ['./convocatorias-publicas.scss']
})
export class ConvocatoriasPublicasComponent implements OnInit, OnDestroy {

  convocatorias: ConvocatoriaVM[] = [];
  cargando = true;

  mostrarModal           = false;
  solicitudParaPostular: SolicitudDocente | null = null;

  private wheelTimeout: any;
  private lastWheelTime = 0;

  constructor(
    private convocatoriaService: ConvocatoriaService,
    private router: Router,
    private cdr:    ChangeDetectorRef,
    private el:     ElementRef,
    public logoService: LogoService
  ) {}

  ngOnInit(): void {
    // Fuerza recarga de logo/nombre en cada visita
    this.logoService.cargar();

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

  ngOnDestroy(): void {
    clearTimeout(this.wheelTimeout);
  }

  /** Scroll con rueda del mouse — navega entre cards */
  @HostListener('wheel', ['$event'])
  onWheel(event: WheelEvent): void {
    const now = Date.now();
    if (now - this.lastWheelTime < 600) return; // throttle
    this.lastWheelTime = now;

    const cards = this.el.nativeElement.querySelectorAll('.conv-card');
    if (!cards.length) return;

    // Encuentra el card más cercano al viewport
    let closestIdx = 0;
    let minDist = Infinity;
    cards.forEach((card: Element, i: number) => {
      const rect = card.getBoundingClientRect();
      const dist = Math.abs(rect.top - 120);
      if (dist < minDist) { minDist = dist; closestIdx = i; }
    });

    const targetIdx = event.deltaY > 0
      ? Math.min(closestIdx + 1, cards.length - 1)
      : Math.max(closestIdx - 1, 0);

    (cards[targetIdx] as HTMLElement).scrollIntoView({
      behavior: 'smooth', block: 'start'
    });
  }

  toggleConvocatoria(conv: ConvocatoriaVM): void {
    if (conv.expandida) {
      conv.expandida             = false;
      conv.solicitudSeleccionada = null;
      return;
    }
    this.convocatorias.forEach(c => {
      c.expandida             = false;
      c.solicitudSeleccionada = null;
    });
    conv.expandida = true;
    if (conv.solicitudes.length > 0) return;
    conv.cargandoSolicitudes = true;
    this.convocatoriaService.obtenerSolicitudes(conv.idConvocatoria).subscribe({
      next: (data) => {
        conv.solicitudes           = data;
        conv.cargandoSolicitudes   = false;
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

  onLogoError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'imgs/logo-uteq.png';
  }
}
