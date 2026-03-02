import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface OpcionMenu {
  idOpcion: number;
  nombre: string;
  ruta: string;
  orden: number;
  soloLectura: boolean;
}

export interface EstadoAuth {
  token: string | null;
  usuarioApp: string | null;
  roles: string[];
  moduloNombre: string | null;
  moduloRuta: string | null;
  opciones: OpcionMenu[];
  idUsuario: number | null;
  nombreRolApp: string | null;
}

const ESTADO_INICIAL: EstadoAuth = {
  token: null, usuarioApp: null, roles: [],
  moduloNombre: null, moduloRuta: null,
  opciones: [], idUsuario: null, nombreRolApp: null
};

@Injectable({ providedIn: 'root' })
export class AuthStateService {

  private estado$ = new BehaviorSubject<EstadoAuth>(this.cargarDeStorage());

  // Guardar estado al hacer login
  setEstado(estado: EstadoAuth): void {
    localStorage.setItem('authState', JSON.stringify(estado));
    this.estado$.next(estado);
  }

  // Limpiar al hacer logout
  limpiar(): void {
    localStorage.removeItem('authState');
    localStorage.removeItem('token');
    localStorage.removeItem('rol');
    this.estado$.next(ESTADO_INICIAL);
  }

  getEstado(): EstadoAuth { return this.estado$.getValue(); }
  getToken(): string | null { return this.getEstado().token; }
  isAutenticado(): boolean { return !!this.getEstado().token; }

  // Verificar si una ruta está en las opciones permitidas
  rutaPermitida(ruta: string): boolean {
    const opciones = this.getEstado().opciones;
    if (!opciones || opciones.length === 0) return false;
    // Normalizar rutas para comparación
    const rutaNorm = ruta.startsWith('/') ? ruta.substring(1) : ruta;
    return opciones.some(o => {
      const optRuta = o.ruta.startsWith('/') ? o.ruta.substring(1) : o.ruta;
      return optRuta === rutaNorm || rutaNorm.startsWith(optRuta + '/');
    });
  }

  esSoloLectura(ruta: string): boolean {
    const opciones = this.getEstado().opciones;
    const rutaNorm = ruta.startsWith('/') ? ruta.substring(1) : ruta;
    const opcion = opciones.find(o => {
      const optRuta = o.ruta.startsWith('/') ? o.ruta.substring(1) : o.ruta;
      return optRuta === rutaNorm;
    });
    return opcion?.soloLectura ?? false;
  }

  private cargarDeStorage(): EstadoAuth {
    try {
      const raw = localStorage.getItem('authState');
      return raw ? JSON.parse(raw) : ESTADO_INICIAL;
    } catch { return ESTADO_INICIAL; }
  }
}
