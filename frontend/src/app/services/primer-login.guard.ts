import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from './auth.service';

// ─── Guard para /cambiar-clave-obligatorio ──────────────────
// Solo permite acceso si el usuario está logueado Y es primer login
// Si no es primer login, redirige a su dashboard según rol

export const PrimerLoginGuard: CanActivateFn = () => {
  const router  = inject(Router);
  const authSvc = inject(AuthService);

  // Sin token → al login
  if (!authSvc.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  // Ya cambió su clave → redirigir a su dashboard
  if (!authSvc.esPrimerLogin()) {
    authSvc.redirigirPorRol();
    return false;
  }

  return true;
};

// ─── Guard para rutas normales ──────────────────────────────
// Si es primer login, bloquea el acceso a cualquier ruta normal
// y obliga a pasar por el cambio de clave

export const NoPrimerLoginGuard: CanActivateFn = (route) => {
  const router  = inject(Router);
  const authSvc = inject(AuthService);

  // Sin token → al login
  if (!authSvc.isLoggedIn()) {
    router.navigate(['/login']);
    return false;
  }

  // Es primer login → forzar cambio de clave
  if (authSvc.esPrimerLogin()) {
    router.navigate(['/cambiar-clave-obligatorio']);
    return false;
  }

  return true;
};
