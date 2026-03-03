import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';

const LOGO_KEY = 'inst_logo_url';
const NOMBRE_KEY = 'inst_nombre';

@Injectable({ providedIn: 'root' })
export class LogoService {

  // Inicializa con lo que haya en caché para respuesta inmediata
  private logo$  = new BehaviorSubject<string | null>(localStorage.getItem(LOGO_KEY));
  private nombre$ = new BehaviorSubject<string>(localStorage.getItem(NOMBRE_KEY) ?? 'SSDC');
  private cargado = false;

  constructor(private http: HttpClient) {
    this.cargar();
  }

  /** Llama al backend UNA sola vez por ciclo de vida del servicio */
  cargar(): void {
    if (this.cargado) return;
    this.cargado = true;
    this.http.get<any>('/api/instituciones/activa').subscribe({
      next: cfg => {
        const url = cfg.logoUrl ?? null;
        this.logo$.next(url);
        this.nombre$.next(cfg.appName ?? cfg.nombreInstitucion ?? 'SSDC');
        // Guardar en caché para la próxima recarga
        if (url) localStorage.setItem(LOGO_KEY, url);
        else      localStorage.removeItem(LOGO_KEY);
        if (cfg.appName) localStorage.setItem(NOMBRE_KEY, cfg.appName);
      },
      error: () => {
        // Si falla, quedamos con el valor en caché (ya está en logo$)
        console.warn('[LogoService] No se pudo cargar configuración institucional');
      }
    });
  }

  // Getters para los componentes
  getLogo()   { return this.logo$.asObservable(); }
  getNombre() { return this.nombre$.asObservable(); }

  // Para uso síncrono en template sin async pipe
  getLogoActual(): string | null { return this.logo$.getValue(); }
}
