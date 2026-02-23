import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

export const AuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  const token =
    localStorage.getItem('token') ||
    sessionStorage.getItem('token');

  const rol = localStorage.getItem('rol');

  // 🔓 Rutas públicas
  if (state.url === '/login' ||
    state.url === '/registro' ||
    state.url === '/recuperar-clave') {
    return true;
  }

  // 🚫 No autenticado
  if (!token || !rol) {
    router.navigate(['/login']);
    return false;
  }

  // 🎯 Validación de rol
  const rolRequerido = route.data?.['rol'];
  if (rolRequerido && rol !== rolRequerido) {
    router.navigate(['/sin-acceso']);
    return false;
  }

  return true;
};
