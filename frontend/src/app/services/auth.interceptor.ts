import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthStateService } from './auth-state.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token     = localStorage.getItem('token');
  const router    = inject(Router);
  const authState = inject(AuthStateService);

  const rutasPublicas = ['/api/auth/login','/api/registro','/api/auth/recuperar'];
  const esPublica = rutasPublicas.some(r => req.url.includes(r));

  const authReq = (token && !esPublica)
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError(err => {
      if (err.status === 401) {
        const esLogout  = req.url.includes('/api/auth/logout');
        const yaEnLogin = router.url.startsWith('/login');
        if (!esLogout && !yaEnLogin) {
          authState.limpiar();
          localStorage.clear();
          router.navigate(['/login'], {
            replaceUrl: true,
            queryParams: { sesionExpirada: 'true' }
          });
        }
      }
      if (err.status === 403) router.navigate(['/sin-acceso']);
      if (err.status === 500) console.error('[Server]', err.url, err.error);
      return throwError(() => err);
    })
  );
};
