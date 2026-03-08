import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';

const LOGO_KEY   = 'inst_logo_url';
const NOMBRE_KEY = 'inst_nombre';
const CACHE_KEY  = 'cache_logo_service';
const TTL_MS     = 24 * 60 * 60 * 1000; // 24 horas

function cacheSet(data: { logoUrl: string | null; appName: string }): void {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({ ts: Date.now(), data }));
  } catch { /* storage lleno */ }
}

function cacheGet(): { logoUrl: string | null; appName: string } | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    const { ts, data } = JSON.parse(raw);
    if (Date.now() - ts > TTL_MS) { localStorage.removeItem(CACHE_KEY); return null; }
    return data;
  } catch { return null; }
}

@Injectable({ providedIn: 'root' })
export class LogoService {

  private logo$   = new BehaviorSubject<string | null>(localStorage.getItem(LOGO_KEY));
  private nombre$ = new BehaviorSubject<string>(localStorage.getItem(NOMBRE_KEY) ?? 'SSDC');

  constructor(private http: HttpClient) {
    // 1. Aplicar caché con TTL al instante si existe
    const cached = cacheGet();
    if (cached) {
      this.logo$.next(cached.logoUrl);
      this.nombre$.next(cached.appName);
    }

    // 2. Refrescar desde el backend en background
    this.cargar();
  }

  cargar(): void {
    this.http.get<any>('/api/instituciones/activa').subscribe({
      next: cfg => {
        const logoUrl = cfg.logoUrl ?? null;
        const appName = cfg.appName ?? cfg.nombreInstitucion ?? 'SSDC';

        // Guardar en caché con TTL
        cacheSet({ logoUrl, appName });

        // Actualizar localStorage legacy
        if (logoUrl) localStorage.setItem(LOGO_KEY, logoUrl);
        else         localStorage.removeItem(LOGO_KEY);
        localStorage.setItem(NOMBRE_KEY, appName);

        // Emitir con cache-buster solo en la URL de la imagen
        if (logoUrl) {
          const sep = logoUrl.includes('?') ? '&' : '?';
          this.logo$.next(`${logoUrl}${sep}_t=${Date.now()}`);
        } else {
          this.logo$.next(null);
        }
        this.nombre$.next(appName);
      },
      error: () => {
      }
    });
  }

  getLogo()        { return this.logo$.asObservable(); }
  getNombre()      { return this.nombre$.asObservable(); }
  getLogoActual()  { return this.logo$.getValue(); }
  getNombreActual(){ return this.nombre$.getValue(); }
}
