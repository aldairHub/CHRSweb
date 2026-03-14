import { Component, OnInit, OnDestroy, ChangeDetectorRef, NgZone } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LogoService } from '../services/logo.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="app-footer">
      <div class="footer-left">
        <span class="footer-inst">{{ appSubtitulo }}</span>
      </div>
      <div class="footer-center">
        <span>© {{ anio }} {{ nombreInstitucion }} — Todos los derechos reservados</span>
      </div>
      <div class="footer-right">
        <span class="footer-time">{{ horaActual }}</span>
      </div>
    </footer>
  `,
  styles: [`
    :host { display: block; }

    .app-footer {
      position: fixed;
      bottom: 0; left: 0; right: 0;
      height: 38px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 24px;
      background: linear-gradient(135deg, rgba(0,122,46,0.97) 0%, rgba(0,166,62,0.93) 100%);
      border-top: 1px solid rgba(255,255,255,0.15);
      color: #fff;
      font-size: 12px;
      z-index: 1000;
      box-shadow: 0 -2px 16px rgba(0,100,30,0.18);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
    }

    .footer-left  { flex: 1; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; opacity: 0.95; }
    .footer-center { flex: 1; text-align: center; opacity: 0.85; font-size: 11.5px; }
    .footer-right { flex: 1; display: flex; align-items: center; justify-content: flex-end; }

    .footer-time {
      font-weight: 700;
      font-size: 17px;
      font-variant-numeric: tabular-nums;
      letter-spacing: 0.6px;
    }

    /* Dark mode — exactamente igual que el navbar */
    :host-context(html.dark) .app-footer,
    :host-context(html.dark) footer {
      background: rgba(0,0,0,0.92) !important;
      border-top: 1px solid rgba(0,210,80,0.12) !important;
      box-shadow: 0 -1px 0 rgba(0,210,80,0.08), 0 -4px 32px rgba(0,0,0,0.9) !important;
      backdrop-filter: blur(20px) saturate(1.5) !important;
      -webkit-backdrop-filter: blur(20px) saturate(1.5) !important;
    }
  `]
})
export class FooterComponent implements OnInit, OnDestroy {

  horaActual   = '';
  nombreInstitucion = 'Universidad';
  appSubtitulo      = 'Sistema de Selección Docente';
  anio         = new Date().getFullYear();

  private timer: any;

  constructor(
    private logoService: LogoService,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone
  ) {}

  ngOnInit() {
    // Cargar desde caché inmediatamente
    const cachedInst = localStorage.getItem('inst_nombreInstitucion');
    const cachedApp  = localStorage.getItem('inst_appName');
    if (cachedInst) this.nombreInstitucion = cachedInst;
    if (cachedApp)  this.appSubtitulo      = cachedApp;

    // Suscribirse a cambios del servicio
    this.logoService.getNombreInstitucion().subscribe(inst => {
      if (inst) this.nombreInstitucion = inst;
      this.cdr.markForCheck();
    });
    this.logoService.getNombre().subscribe(nombre => {
      this.appSubtitulo = localStorage.getItem('inst_appName') || nombre || 'Sistema de Selección Docente';
      this.cdr.markForCheck();
    });

    // Timer dentro de NgZone para que Angular detecte cambios automáticamente
    this.ngZone.runOutsideAngular(() => {
      this.actualizarHora();
      this.timer = setInterval(() => {
        this.ngZone.run(() => {
          this.actualizarHora();
          this.cdr.detectChanges();
        });
      }, 1000);
    });
  }

  ngOnDestroy() {
    clearInterval(this.timer);
  }

  private actualizarHora() {
    const now = new Date();
    this.horaActual = now.toLocaleTimeString('es-EC', {
      hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: true
    });
  }
}
