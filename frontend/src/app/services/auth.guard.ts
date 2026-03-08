import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthStateService } from './auth-state.service';

export const AuthGuard: CanActivateFn = (route, state) => {
  const router    = inject(Router);
  const authState = inject(AuthStateService);

  const RUTAS_TRANSVERSALES = [
    '/login', '/registro', '/recuperar-clave', '/sin-acceso',
    '/cambiar-clave-obligatorio', '/cambio-clave-obligatorio',
    '/perfil', '/notificaciones',
  ];

  if (RUTAS_TRANSVERSALES.some(r => state.url.startsWith(r))) return true;

  if (!authState.isAutenticado()) {
    router.navigate(['/login']);
    return false;
  }

  const esHome = route.data?.['isHome'] === true;
  if (esHome) return true;

  // Permitir raiz del modulo Y TODAS sus sub-rutas
  const moduloRuta = authState.getEstado().moduloRuta;
  if (moduloRuta) {
    const raizModulo   = '/' + moduloRuta.replace(/^\//, '');
    const urlSinParams = state.url.split('?')[0];
    if (urlSinParams === raizModulo ||
        urlSinParams === raizModulo + '/' ||
        urlSinParams.startsWith(raizModulo + '/')) {
      return true;
    }
  }

  // Sin opciones configuradas -> permitir
  const opciones = authState.getEstado().opciones;
  if (!opciones || opciones.length === 0) return true;

  const rutaSinParams = state.url.split('?')[0];
  if (!authState.rutaPermitida(rutaSinParams)) {
    router.navigate(['/sin-acceso']);
    return false;
  }

  return true;
};
