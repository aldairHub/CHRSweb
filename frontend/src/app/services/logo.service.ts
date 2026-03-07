import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';

const LOGO_KEY   = 'inst_logo_url';
const NOMBRE_KEY = 'inst_nombre';

@Injectable({ providedIn: 'root' })
export class LogoService {

  private logo$   = new BehaviorSubject<string | null>(localStorage.getItem(LOGO_KEY));
  private nombre$ = new BehaviorSubject<string>(localStorage.getItem(NOMBRE_KEY) ?? 'SSDC');

  constructor(private http: HttpClient) {
    this.cargar();
  }

  /** Siempre busca datos frescos del backend, sin bloquear con flag */
  cargar(): void {
    const ts = Date.now(); // cache buster
    this.http.get<any>(`/api/instituciones/activa?_t=${ts}`).subscribe({
      next: cfg => {
        let url = cfg.logoUrl ?? null;
        // Añadir cache-buster al URL de la imagen para forzar recarga
        if (url) {
          const sep = url.includes('?') ? '&' : '?';
          url = `${url}${sep}_t=${ts}`;
        }
        this.logo$.next(url);
        this.nombre$.next(cfg.appName ?? cfg.nombreInstitucion ?? 'SSDC');
        if (cfg.logoUrl) localStorage.setItem(LOGO_KEY, cfg.logoUrl);
        else             localStorage.removeItem(LOGO_KEY);
        if (cfg.appName) localStorage.setItem(NOMBRE_KEY, cfg.appName);
      },
      error: () => {
        console.warn('[LogoService] No se pudo cargar configuración institucional');
      }
    });
  }

  getLogo()        { return this.logo$.asObservable(); }
  getNombre()      { return this.nombre$.asObservable(); }
  getLogoActual()  { return this.logo$.getValue(); }
  getNombreActual(){ return this.nombre$.getValue(); }
}
