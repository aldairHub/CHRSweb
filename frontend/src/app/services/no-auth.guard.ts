import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
export const NoAuthGuard: CanActivateFn = () => {
  const router = inject(Router);
  const token = localStorage.getItem('token');
  const rol   = localStorage.getItem('rol');

  if (token && rol) {
    return router.parseUrl(`/${rol}`);
  }

  return true;
};
