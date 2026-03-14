import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';

const LOGO_KEY   = 'inst_logo_url';
const NOMBRE_KEY = 'inst_nombre';
const CACHE_KEY  = 'cache_logo_service';
const TTL_MS     = 24 * 60 * 60 * 1000; // 24 horas

function cacheSet(data: { logoUrl: string | null; appName: string; nombreCorto?: string; nombreInstitucion?: string }): void {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({ ts: Date.now(), data }));
  } catch { /* storage lleno */ }
}

function cacheGet(): { logoUrl: string | null; appName: string; nombreCorto?: string; nombreInstitucion?: string } | null {
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

  private logo$             = new BehaviorSubject<string | null>(localStorage.getItem(LOGO_KEY));
  private nombre$           = new BehaviorSubject<string>(localStorage.getItem(NOMBRE_KEY) ?? 'SSDC');
  private nombreCorto$      = new BehaviorSubject<string>(localStorage.getItem('inst_nombreCorto') ?? '');
  private nombreInstitucion$ = new BehaviorSubject<string>(localStorage.getItem('inst_nombreInstitucion') ?? '');

  constructor(private http: HttpClient) {
    // 1. Aplicar caché con TTL al instante si existe
    const cached = cacheGet();
    if (cached) {
      this.logo$.next(cached.logoUrl);
      this.nombre$.next(cached.appName);
      if (cached.nombreCorto) this.nombreCorto$.next(cached.nombreCorto);
      if (cached.nombreInstitucion) this.nombreInstitucion$.next(cached.nombreInstitucion);
    }

    // 2. Refrescar desde el backend en background
    this.cargar();
  }

  cargar(): void {
    this.http.get<any>('/api/instituciones/activa').subscribe({
      next: cfg => {
        const logoUrl          = cfg.logoUrl ?? null;
        const appName          = cfg.appName ?? cfg.nombreInstitucion ?? 'SSDC';
        const nombreCorto      = cfg.nombreCorto ?? '';
        const nombreInstitucion = cfg.nombreInstitucion ?? appName;

        // Guardar en caché con TTL
        cacheSet({ logoUrl, appName, nombreCorto, nombreInstitucion });

        // Actualizar localStorage
        if (logoUrl) localStorage.setItem(LOGO_KEY, logoUrl);
        else         localStorage.removeItem(LOGO_KEY);
        localStorage.setItem(NOMBRE_KEY, appName);
        localStorage.setItem('inst_appName', cfg.appName ?? 'Sistema de selección docente');
        localStorage.setItem('inst_nombreCorto', nombreCorto);
        localStorage.setItem('inst_nombreInstitucion', nombreInstitucion);

        // Actualizar título de la pestaña del navegador
        const shortTitle = nombreCorto || appName || 'SSDC';
        document.title = shortTitle;

        // Actualizar favicon dinámicamente si hay logo
        if (logoUrl) {
          this.actualizarFavicon(logoUrl);
        }

        // Emitir con cache-buster solo en la URL de la imagen
        if (logoUrl) {
          const sep = logoUrl.includes('?') ? '&' : '?';
          this.logo$.next(`${logoUrl}${sep}_t=${Date.now()}`);
        } else {
          this.logo$.next(null);
        }
        this.nombre$.next(appName);
        this.nombreCorto$.next(nombreCorto);
        this.nombreInstitucion$.next(nombreInstitucion);
      },
      error: () => {}
    });
  }

  getLogo()               { return this.logo$.asObservable(); }
  getNombre()             { return this.nombre$.asObservable(); }
  getNombreCorto()        { return this.nombreCorto$.asObservable(); }
  getNombreInstitucion()  { return this.nombreInstitucion$.asObservable(); }
  getLogoActual()         { return this.logo$.getValue(); }
  getNombreActual()       { return this.nombre$.getValue(); }

  private actualizarFavicon(url: string): void {
    try {
      let link = document.querySelector<HTMLLinkElement>('link[rel~="icon"]');
      if (!link) {
        link = document.createElement('link');
        link.rel = 'icon';
        document.head.appendChild(link);
      }
      // Usar el logo como favicon (funciona mejor con PNG)
      link.type = 'image/png';
      const sep = url.includes('?') ? '&' : '?';
      link.href = `${url}${sep}_fav=${Date.now()}`;
    } catch { /* ignorar errores de favicon */ }
  }
}
