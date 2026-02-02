import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

export const AuthGuard: CanActivateFn = (route, state) => {
  const router = inject(Router);

  const rol = localStorage.getItem('rol');

  if (!rol) {
    router.navigate(['/login']);
    return false;
  }

  const rolRequerido = route.data['rol'];

  if (rolRequerido && rol !== rolRequerido) {
    router.navigate(['/login']);
    return false;
  }

  return true;
};
