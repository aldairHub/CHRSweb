import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthStateService } from './auth-state.service';

export const AuthGuard: CanActivateFn = (route, state) => {
  const router    = inject(Router);
  const authState = inject(AuthStateService);

  // Rutas públicas — siempre permitir sin importar el estado
  const rutasPublicas = [
    '/login',
    '/registro',
    '/recuperar-clave',
    '/sin-acceso',
    '/cambiar-clave-obligatorio',  // ✅ debe ser pública: el usuario llega aquí antes de tener opciones
    '/cambio-clave-obligatorio',
  ];
  if (rutasPublicas.some(r => state.url.startsWith(r))) return true;

  // Sin autenticación → login
  if (!authState.isAutenticado()) {
    router.navigate(['/login']);
    return false;
  }

  // Rutas marcadas como "home" del módulo no requieren validar opciones
  // (el módulo /admin, /evaluador, /postulante, etc.)
  const esHome = route.data?.['isHome'] === true;
  if (esHome) return true;

  // Para rutas sin opciones configuradas (usuario sin menú asignado), permitir igual
  // La seguridad real es el backend — el guard solo mejora la UX
  const opciones = authState.getEstado().opciones;
  if (!opciones || opciones.length === 0) return true;

  // Verificar si la ruta está en las opciones permitidas del usuario
  const rutaSinParams = state.url.split('?')[0];
  if (!authState.rutaPermitida(rutaSinParams)) {
    router.navigate(['/sin-acceso']);
    return false;
  }

  return true;
};
