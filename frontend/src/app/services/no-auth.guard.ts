import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const NoAuthGuard: CanActivateFn = () => {

  const router = inject(Router);

  const token = localStorage.getItem('token');
  const rol = localStorage.getItem('rol');

  if (token) {

    if (rol === 'admin')
      return router.parseUrl('/admin');

    if (rol === 'evaluador')
      return router.parseUrl('/evaluador');

    if (rol === 'postulante')
      return router.parseUrl('/postulante');

    return router.parseUrl('/'); // fallback
  }

  return true;
};
